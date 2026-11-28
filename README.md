# Juegos de casa

Juegos sencillos para jugar en la tele (con el mando), en el iPad o en el ordenador.

- **Collar de Colores** (`bolas/`): junta 3 bolas del mismo color para que exploten.
- **Las estrellas perdidas** (`aventura/`): aventura para 2 jugadores (Diego y Sofía) con Tito el dinosaurio. Cada mando es un jugador. Nombres cambiables con `?n1=…&n2=…`.
  - 30 niveles en 5 mundos (pradera, montaña de hielo, templo, valle de los colores y castillo de las nubes) y 5 niveles secretos que se abren con la estrella dorada escondida en cada mundo. El mapa recuerda hasta dónde se ha llegado.
  - Botones: A = magia (o pintar un puente), X = golpe (rompe paredes agrietadas y quita cascos), Y = deshacer (pulsar y soltar), mantener Y = empezar el nivel de nuevo. B no hace nada al jugar (se pulsaba sin querer).
  - Pruebas: `?nivel=N` abre el nivel N (31-35 son los secretos), `&solo=1` con un jugador, `?mapa=todo` abre el mapa con todo desbloqueado.

Web: https://sactre.github.io/juegos/

## Versiones

Cada juego lleva su versión en `<meta name="version" content="N (fecha)">` (también la portada). Súbela en cada cambio: la portada la lee del mismo archivo que se va a abrir y la enseña debajo de cada juego, y cada juego la pinta en su pantalla de título. Así, tras "Buscar actualizaciones", se ve si ha llegado la versión nueva. La portada también muestra la versión de la app de la tele.
