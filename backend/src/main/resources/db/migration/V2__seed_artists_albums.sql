INSERT INTO artist (id, name) VALUES
    (1, 'Serj Tankian'),
    (2, 'Mike Shinoda'),
    (3, 'Michel Teló'),
    (4, 'Guns N'' Roses');

SELECT setval(pg_get_serial_sequence('artist', 'id'), (SELECT MAX(id) FROM artist));

INSERT INTO album (artist_id, title) VALUES
    (1, 'Harakiri'),
    (1, 'Black Blooms'),
    (1, 'The Rough Dog'),
    (2, 'The Rising Tied'),
    (2, 'Post Traumatic'),
    (2, 'Post Traumatic EP'),
    (2, 'Where''d You Go'),
    (3, 'Bem Sertanejo'),
    (3, 'Bem Sertanejo - O Show (Ao Vivo)'),
    (3, 'Bem Sertanejo - (1ª Temporada) - EP'),
    (4, 'Use Your Illusion I'),
    (4, 'Use Your Illusion II'),
    (4, 'Greatest Hits');
