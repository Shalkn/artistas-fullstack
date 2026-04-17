package com.artistas.api.service;

import com.artistas.api.config.AppProperties;
import com.artistas.api.domain.Regional;
import com.artistas.api.dto.regional.RegionalDto;
import com.artistas.api.repository.RegionalRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sincronização com a API externa de regionais: inserir novos, inativar ausentes,
 * versionar alteração de denominação (inativar + novo registro ativo).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegionalSyncService {

    private final RegionalRepository regionalRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    /**
     * Obtém o JSON de regionais via GET, reconcilia com a base local e retorna estatísticas.
     * Regras: códigos ausentes na API são inativados; mudança de nome para mesmo código gera novo registro ativo
     * e inativa o anterior (histórico).
     *
     * @throws IllegalStateException    rede/HTTP ou corpo vazio (vira 502 no handler global)
     * @throws IllegalArgumentException JSON em formato não suportado (400)
     */
    @Transactional
    public SyncResult syncFromArgus() {
        String url = appProperties.getArgus().getRegionaisUrl();
        String body;
        try {
            body = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("Falha ao consultar Argus: {}", e.getMessage());
            throw new IllegalStateException("Não foi possível obter regionais da API externa", e);
        }
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Resposta vazia da API de regionais");
        }

        List<RemoteRegional> remotes = parseRegionaisJson(body);
        Map<Integer, String> byCodigo = new HashMap<>();
        for (RemoteRegional r : remotes) {
            if (r.codigo() != null && r.nome() != null && !r.nome().isBlank()) {
                byCodigo.put(r.codigo(), r.nome().trim());
            }
        }

        List<Integer> codigosRemotos = new ArrayList<>(byCodigo.keySet());

        if (codigosRemotos.isEmpty()) {
            regionalRepository.deactivateAllActive();
        } else {
            regionalRepository.deactivateActiveNotInCodigos(codigosRemotos);
        }

        int criados = 0;
        int atualizadosVersao = 0;

        for (Map.Entry<Integer, String> e : byCodigo.entrySet()) {
            Integer codigo = e.getKey();
            String nome = e.getValue();
            var ativoOpt = regionalRepository.findActiveByCodigoExterno(codigo);
            if (ativoOpt.isEmpty()) {
                regionalRepository.save(Regional.builder()
                        .codigoExterno(codigo)
                        .nome(nome)
                        .ativo(true)
                        .build());
                criados++;
            } else {
                Regional atual = ativoOpt.get();
                if (!atual.getNome().equals(nome)) {
                    regionalRepository.deactivateById(atual.getId());
                    regionalRepository.save(Regional.builder()
                            .codigoExterno(codigo)
                            .nome(nome)
                            .ativo(true)
                            .build());
                    atualizadosVersao++;
                }
            }
        }

        return new SyncResult(remotes.size(), criados, atualizadosVersao);
    }

    /**
     * Aceita array na raiz ou objeto com {@code data} ou {@code content} contendo o array.
     */
    private List<RemoteRegional> parseRegionaisJson(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode array = root.isArray() ? root : root.path("data");
            if (!array.isArray()) {
                array = root.path("content");
            }
            if (!array.isArray()) {
                throw new IllegalArgumentException("Formato JSON não reconhecido (esperado array ou objeto com data/content)");
            }
            List<RemoteRegional> list = new ArrayList<>();
            for (JsonNode node : array) {
                Integer codigo = firstInt(node, "id", "codigo", "codigoRegional", "regionalId");
                String nome = firstText(node, "nome", "descricao", "nomeRegional", "denominacao");
                if (codigo != null && nome != null) {
                    list.add(new RemoteRegional(codigo, nome));
                }
            }
            return list;
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON de regionais inválido", ex);
        }
    }

    /** Retorna o primeiro campo listado que existir e for inteiro JSON. */
    private Integer firstInt(JsonNode node, String... keys) {
        for (String k : keys) {
            if (node.hasNonNull(k) && node.get(k).canConvertToInt()) {
                return node.get(k).asInt();
            }
        }
        return null;
    }

    /** Retorna o primeiro campo textual entre os aliases possíveis. */
    private String firstText(JsonNode node, String... keys) {
        for (String k : keys) {
            if (node.hasNonNull(k) && node.get(k).isTextual()) {
                return node.get(k).asText();
            }
        }
        return null;
    }

    /** Lista apenas regionais com {@code ativo = true} (ordem definida pelo repositório / JPA). */
    @Transactional(readOnly = true)
    public List<RegionalDto> listAtivas() {
        return regionalRepository.findAllActive().stream()
                .map(r -> new RegionalDto(r.getId(), r.getCodigoExterno(), r.getNome(), r.isAtivo()))
                .toList();
    }

    private record RemoteRegional(Integer codigo, String nome) {
    }

    /**
     * Resultado da sincronização: totais para auditoria e testes manuais.
     *
     * @param recebidosDaApi            quantidade de itens interpretados do JSON remoto
     * @param inseridos                 novos registros ativos criados
     * @param versionadosPorMudancaNome linhas inativadas + novas por mudança de nome no mesmo código
     */
    public record SyncResult(int recebidosDaApi, int inseridos, int versionadosPorMudancaNome) {
    }
}
