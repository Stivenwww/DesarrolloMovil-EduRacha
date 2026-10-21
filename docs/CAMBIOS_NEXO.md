# EduRacha · Rediseño "Nexo" (fase 3)

Diseño elegido: **Opción E · Nexo**, islas flotantes en la era de la IA.
- **Modo oscuro**: Nexo original (noche, neón cian, violeta y verde).
- **Modo claro**: Nexo con los colores de la opción D (mar, arena, cian, verde y ámbar).
- El usuario elige **Sistema / Claro / Oscuro** en *Configuración → Apariencia*. La elección se guarda en el celular y se sincroniza con su perfil en el servidor.

## Cómo compilar
1. Abre `DesarrolloMovil-EduRacha` en Android Studio.
2. Ejecuta **Build → Clean Project** y luego **Build → Rebuild Project**.
3. Si aparece un error, copia el mensaje completo y compártelo. Nada de esto se pudo compilar desde aquí: solo se revisó que las llaves y los paréntesis estuvieran balanceados.

Backend: `./gradlew build`, luego despliega igual que siempre. Las variables de entorno no cambian.

## Frontend: qué cambió

### Arquitectura del tema
| Archivo | Rol |
|---|---|
| `ui/theme/Nexo.kt` | Paletas `NexoOscuro` y `NexoClaro`, más `TemaApp` (preferencia observable y persistencia). |
| `ui/theme/NexoComponentes.kt` | Robi Neo animado, `FondoNexo`, `IslaFlotante`, `HexNodo`, `BotonNexo`, `TarjetaNexo`, `ChipNexo`, `BarraNexo`, `EstrellasNexo` y `SelectorTemaNexo`. |
| `ui/theme/NexoPantallas.kt` (nuevo) | Encabezado, diálogos (`DialogoNexo` y `DialogoAccionNexo`), `GloboRobi`, `StatNexo`, `FilaNexo`, `AvatarNexo`, estados vacíos y de carga. |
| `Color.kt`, `EduRacha 2.0.kt`, `EduRachaPlay.kt`, `EduRachaDesignSystem.kt` | Los colores antiguos ahora leen la paleta activa, así que las pantallas viejas también cambian de modo. |
| `services/GestorSonidos.kt` + `res/raw/sonido_*.wav` | Sonidos de acierto, error, fin y avance. Respetan el interruptor de sonidos. |
| `services/SincronizadorPreferencias.kt` (nuevo) | Sube y descarga el tema y los sonidos del perfil. Si falla, la app sigue con la copia local. |

### Pantallas rediseñadas (la lógica, los ViewModel y los Intent no cambian)
- **Bienvenida y login**: Robi saluda sobre su isla y el formulario va en una tarjeta Nexo.
  - El teclado pasa de campo con *Siguiente* e inicia sesión con *Listo*.
  - Se dejó de registrar el correo en los logs.
- **Inicio del estudiante**:
  - Tarjeta principal con Robi y la acción "Continuar aprendiendo".
  - Estadísticas reales.
  - Se quitaron los "puntos" inventados (cursos × 50).
- **Temas del curso**: mapa de islas flotantes con nodos hexagonales en zigzag y progreso del curso.
  - Se quitó el "+XP" inventado por tema.
- **Explicación interactiva**: cada párrafo es una tarjeta.
  - Las ideas se revelan una por una.
  - Antes de avanzar hay un mini reto de Robi, "completa la idea", que se genera localmente a partir del texto. Si se falla, no se pierden vidas.
  - Botón para escuchar la tarjeta (TextToSpeech del sistema, sin permisos).
  - Se corrigió un cierre de la app cuando el tema no tenía explicación.
- **Quiz**:
  - Barra de progreso animada y opciones con letra A, B, C, D.
  - Animación de "salto" al acertar y de temblor al fallar.
  - Estrellas que estallan alrededor de Robi.
  - Sonido de acierto y de error.
  - Contador de aciertos seguidos (solo visual).
  - Se quitó el "+50 XP" fijo, que no venía del servidor.
- **Resultados**:
  - 0 a 3 estrellas (90% = 3, 80% = 2, 50% = 1).
  - La XP del servidor se muestra con un conteo animado.
  - Sonido final.
  - El confeti se arregló: antes no se movía.
- **Ranking**:
  - Podio en escalones sobre islas: 2.º, 1.º y 3.º.
  - Tarjeta "Tu posición" que dice cuánto falta para subir un puesto.
  - Corregido: con 1 o 2 estudiantes antes no aparecía nadie.
- **Panel docente**:
  - Tarjeta "Asistente IA" con Robi, que lleva a revisar preguntas pendientes o a crear preguntas con IA.
  - Estadísticas y herramientas con colores Nexo.
- **Emojis**: se quitaron todos los de la interfaz y se reemplazaron por iconos o por Robi. Solo quedan en los logs internos.
- **Resto de pantallas**: los fondos blancos, textos oscuros y grises fijos se cambiaron por colores del tema. Los textos e iconos blancos sobre botones o encabezados de color se dejaron igual.

## Backend: qué cambió
| Cambio | Detalle |
|---|---|
| `routes/PreferenciasRoutes.kt` (nuevo) | `GET` y `PUT /api/usuarios/me/preferencias`. <ul><li>El UID se toma del token de Firebase.</li><li>Lista blanca de valores: `SISTEMA`, `CLARO`, `OSCURO`.</li><li>Responde 204 si aún no hay nada guardado.</li></ul> |
| `utils/Autorizacion.kt` (nuevo) | `exigirUsuario()`, `exigirRol(...)` y `esDuenoOPrivilegiado(...)`. |
| `routes/preguntaRoutes.kt` | Todas las rutas exigen rol docente o admin. Antes cualquiera podía ver las respuestas correctas, borrar preguntas o aprobarlas. |
| `routes/SolicitudCursoRoutes.kt` | <ul><li>`unirse`: solo se puede enviar a nombre propio.</li><li>Las solicitudes del estudiante solo las ve él mismo o un docente.</li><li>Las de un docente solo las ve ese docente.</li><li>Responder, cambiar el estado o regenerar vidas: solo docente o admin.</li></ul> |
| `routes/UsuarioRoutes.kt` | <ul><li>`PUT /usuario/me` ya no permite cambiar el `rol`. Antes un estudiante podía volverse admin.</li><li>`DELETE /usuario/{uid}` queda solo para admin.</li></ul> |

Copias de seguridad:
- Frontend: `_respaldo_rediseno/fase3/`. `java_antes_nexo.tar` contiene el código antes del tema Nexo y `java_pre_pantallas.tar` el código antes de rediseñar las pantallas.
- Backend: `_respaldo_correcciones/fase3/`.

## Fase 3b (continuación)

### Verificación real de compilación
Se compiló **todo** el código con el compilador oficial de Kotlin y con las mismas librerías que usa el proyecto, copiadas de tu carpeta `.gradle`:
- **App Android:** 0 errores. `R` y `BuildConfig` se simularon, porque los genera Gradle.
- **Backend:** 0 errores.

Además, **Build → Assemble Project en tu Android Studio terminó con BUILD SUCCESSFUL** (APK de depuración generado, solo advertencias de iconos obsoletos). Falta probarla en el celular.

### Nuevo
- **Backend: `GET /quiz/aprobados/{cursoId}`.** Devuelve los temas aprobados (80% o más) y si el quiz final está aprobado, solo para el usuario del token.
  - El mapa de islas usa este endpoint, en lugar de descargar todos los quizzes de Firebase.
  - Mientras el backend nuevo no esté desplegado (respuesta 404), la app usa una consulta filtrada por estudiante como respaldo.
- **Pantallas rediseñadas con Nexo:**
  - Cursos disponibles, Mis cursos y Mis solicitudes.
  - Registro: igual al login, con indicador de fuerza de la contraseña, botón *Listo* del teclado para registrarse y sin datos personales en los logs.
  - Retroalimentación: Robi explica, pantalla de puntaje perfecto con 3 estrellas y botón "¡Lo entendí!".
  - Notificaciones del estudiante y encabezado de Configuración.
- **Barras superiores de las pantallas del docente:** ahora usan violeta oscuro (`EduRachaColors.BarraSuperior`). El cian anterior con texto blanco casi no se leía en modo claro.
- **Reglas de Firebase recomendadas:** `docs/database.rules.recomendadas.json`. Antes de publicarlas en la consola de Firebase:
  - Despliega primero el backend nuevo.
  - Pruébalas en el simulador de reglas: bloquean que un estudiante lea datos de otros, y alguna pantalla que lea datos ajenos directamente podría dejar de funcionar.

### Limpieza recomendada
En `C:\Users\STIVEN\.gradle\eduracha_verificacion` quedaron copias de las librerías (unos 450 MB) que se usaron solo para compilar. Puedes borrar esa carpeta cuando quieras.

## Pendiente o por verificar
- **Probar en el celular o emulador.** La app ya compila en Android Studio (BUILD SUCCESSFUL). El emulador Pixel 9a se cerraba solo, así que no se pudo ver en ejecución.
- **Reglas de Firebase:** aplicar `docs/database.rules.recomendadas.json` después de desplegar el backend (ver arriba).
- **Otros pendientes:**
  - `QuizActivity`: al quedarse sin vidas espera 100 s (`delay(100000)`), pero el log dice 2 s. Se dejó igual para no cambiar la lógica.
  - CORS está en `anyHost()`. Hay que restringirlo al dominio real si existe un cliente web.
  - Versiones: el servidor usa Ktor 2.3.12 y el cliente Ktor 3.3.0. Conviene alinearlas.
- **Pantallas secundarias del docente** (reportes, grupos, crear curso): heredan los colores del tema, pero no se rediseñaron una por una.

## Fase 3e: regla de la explicación

- **Primera vez (inicio del viaje por las islas):** la explicación es obligatoria. El quiz se desbloquea al completar todas las tarjetas.
- **Después:** si el estudiante ya leyó *esa misma versión*, el quiz (oficial y práctica) aparece arriba de inmediato y las tarjetas quedan como repaso opcional, con navegación libre.
- **Si el docente cambia la explicación:** se vuelve a pedir leerla. Aparece el aviso "Explicación actualizada".
  - **Huella:** se guarda una huella del texto en `explicacion_vista/{uid}/{temaId}/huella`.
  - **Cambios de formato:** la huella ignora espacios y marcas de formato, así que limpiar los `**` no cuenta como cambio.
  - **Registros antiguos:** los que no tienen huella se consideran vigentes.
- **Backend:**
  - Nueva ruta `GET /quiz/explicacion-vista/{cursoId}/{temaId}` → `{"vista": bool, "cambio": bool}`.
  - `POST /quiz/explicacion-vista` acepta `cursoId` opcional.
  - `iniciarQuiz` aplica la misma regla.
- **Sin `**`:**
  - La IA ahora recibe la instrucción de responder en texto plano.
  - Toda explicación se guarda sin Markdown (`utils/TextoPlano.kt`).
  - La app también limpia las explicaciones antiguas al mostrarlas.

## Fase 4: correcciones de EduRacha 2.0 (portátil de Laura)

### Errores corregidos (causa → solución)
- **"Acceso denegado" para docentes:** el backend leía el rol de un claim del token que casi nunca existe, así que todos quedaban como "estudiante".
  - Ahora lee `usuarios/{uid}/rol` (con caché de 60 s).
  - Los reportes verifican además que el curso sea del docente.
- **Solicitudes de inscripción:**
  - Se usa el UID del token, no el de las preferencias.
  - El id del curso sale de la clave del nodo.
  - Solo bloquean las solicitudes PENDIENTE o ACEPTADA; una RECHAZADA permite volver a pedir.
  - Detecta si el estudiante ya está inscrito.
  - Los mensajes de error son claros.
- **Quiz final repetido y progreso que no se guardaba:**
  - El backend nunca guardaba `temasCompletados`, porque Firebase no acepta `Set`. Ahora guarda solo los campos que cambian, con listas.
  - El quiz final cuenta como aprobado y no se puede volver a abrir una vez aprobado.
  - El mapa de islas se recarga al volver del quiz.
- **Destellos rojo/verde en las respuestas:** cada opción se pinta con el resultado de su propia pregunta y se limpia el resultado anterior.
- **Ranking docente vacío o colgado:**
  - Lectura tolerante a formatos.
  - Incluye estudiantes sin el campo `estado`.
  - El ranking general suma la experiencia por estudiante.
  - Usa el mismo podio Nexo que el estudiante.
- **Excel:**
  - Cabecera `Content-Disposition` válida con acentos.
  - Celdas numéricas en lugar de texto "NaN".
  - Prueba real de `.xlsx` con acentos.
- **Mensajes del quiz:** antes no se mostraban en ninguna pantalla. Ahora aparecen arriba (`ConMensajesQuiz`).

### Funciones nuevas o rehechas
- **Creación manual de preguntas:**
  - Ya no cierra la pantalla al guardar.
  - Agrega preguntas seguidas, con la lista de la sesión para editar o eliminar.
  - Valida antes de guardar (opciones repetidas, exactamente una correcta).
  - Evita el doble envío.
  - Hasta 6 opciones.
  - El selector muestra solo los cursos del docente.
- **PDF con IA (real):**
  - `POST /api/pdf/extraer` y `POST /api/preguntas/ia/generar-desde-pdf`, con PDFBox 2.0.32.
  - Valida firma, tamaño (20 MB), contraseña, archivo dañado y PDF escaneado.
  - Los documentos largos se dividen en hasta 6 fragmentos repartidos.
  - En Crear curso, el texto extraído pasa al contenido del tema; antes la carga era simulada con `example.com`.
- **Perfil docente:**
  - Estadísticas solo de sus cursos.
  - Sin correo inventado.
  - Nueva sección "Contenido académico" por curso.
- **Modo oscuro y tema:**
  - Chips y filtros seleccionados con el tinte de la marca.
  - Grises fijos reemplazados por los del tema.
  - Avisos inferiores con estilo Nexo (`AvisosNexoHost`).
  - Pantalla de inicio sin destello y con fondo Nexo (`values*/themes.xml`, `values-v31`).
- **Validación de preguntas:** cabecera legible y tarjetas con ancho máximo en tablets.

### Verificación
- **App:** *Build → Assemble Project* en el portátil terminó con **BUILD SUCCESSFUL** (1 min 32 s, solo advertencias de APIs obsoletas).
- **Backend:**
  - Compilado con kotlinc y 27 pruebas JUnit en verde, entre ellas `LectorPdfTest`, `DocumentoIaTest` y `ReportesExcelTest`.
  - PDFBox se verificó con una API de prueba, porque Maven Central no era accesible desde el entorno de trabajo. Al ejecutar `gradlew build` en el PC, Gradle descarga la librería real.

### Pendiente o limitaciones
- **OCR de PDF escaneados:** necesita Tesseract instalado en el servidor. Hoy se informa al docente y no se finge el procesamiento.
- **Pruebas manuales en el emulador:** quedan por hacer con usuarios reales (estudiante y docente). Sin acceso a sus cuentas no se pudo probar el flujo completo de punta a punta.

## Fase 5: ronda de correcciones del rol docente

### Crear preguntas (rehecha con el diseño Nexo)
- Flujo en 3 pasos: curso y tema (en hojas inferiores, con el estado de la explicación del tema), cómo crearlas (Escribirlas / Con IA) y el formulario.
- **Ya no se cierra al guardar.** Las preguntas creadas aparecen abajo ("Creadas ahora") para verlas, editarlas o eliminarlas, con un botón para ir a aprobarlas.
- El estado se conserva al girar la pantalla.
- **Se quitó el PDF de esta pantalla.** El PDF ahora va al crear los temas del curso.
- **Tope con IA: 30 preguntas.** Por arquitectura se generan en **lotes de 10** con una pausa de 4 s entre lotes. Se muestra el avance ("Lote 2 de 3 · 14 de 30") y se puede detener; cada lote se guarda apenas termina.
- Backend:
  - Si la IA responde "límite superado" (429/502/503), espera lo que pide el proveedor y reintenta hasta 4 veces.
  - Como máximo 2 llamadas simultáneas a la IA por servidor (`ai/LimitesIa.kt`).
  - No se repiten preguntas que el tema ya tiene.

### Crear temas: PDF → explicación con IA
- Al agregar un tema (pantalla completa) se puede adjuntar un PDF. El servidor lee su texto y la IA escribe la explicación **solo con ese contenido**.
- La explicación se ve en el mismo formulario y se puede editar antes de guardar.
- Si la IA falla, se puede escribir a mano en el mismo campo.
- El identificador del tema se crea solo a partir del título.
- Nueva ruta: `POST /api/ia/explicacion-borrador`.

### Explicaciones (validar)
- La IA escribe explicaciones **detalladas** (600-900 palabras) por secciones: Introducción, Conceptos clave, Cómo funciona, Ejemplos, Errores comunes y Resumen. Se ciñen al tema y al curso.
- CRUD completo, con filtros por estado. Cada tema se abre en una pantalla de lectura cómoda (ya no en un diálogo amontonado). Botones disponibles:
  - Editar.
  - Guardar.
  - Guardar y aprobar.
  - Aprobar.
  - Rechazar.
  - Regenerar con IA.
  - Desde un PDF.
  - Eliminar.
- Si el tema no tiene explicación o la IA falla, se puede escribir a mano.
- Rutas nuevas:
  - `PUT /api/cursos/{id}/temas/{temaId}/explicacion`
  - `DELETE /api/cursos/{id}/temas/{temaId}/explicacion`
  - `POST /api/cursos/{id}/temas/{temaId}/explicacion-desde-pdf`
- `generar-explicacion` y `validar-explicacion` ahora exigen rol docente o admin.

### Ranking docente: estadísticas
- Nueva vista **Estadísticas** junto al ranking, con datos calculados por el servidor a partir de cada estudiante (`GET /reportes/cursos/{id}/estadisticas`):
  - indicadores;
  - actividad de 14 días;
  - distribución de puntajes;
  - promedio por tema;
  - estudiantes que necesitan apoyo y el motivo;
  - preguntas más difíciles;
  - tabla ordenable por estudiante.

### Reportes
- Los Excel tienen diseño propio de EduRacha (`services/LibroExcelEduRacha.kt`):
  - franja de marca, curso y fecha;
  - indicadores;
  - tabla con encabezado de color, filas alternas, estados de color, filtros y fila fija;
  - gráfico de barras;
  - pie de página;
  - página horizontal para imprimir.
- La vista previa en la app muestra lo mismo que el Excel. Después de descargarlo se puede **abrir** o **compartir**.
- El archivo se guarda en Descargas con un nombre claro (`EduRacha_General_<curso>_<fecha>.xlsx`).

### Avisos y encabezados
- Avisos Nexo (éxito, error, advertencia e información) con icono, título, botón cerrar y barra de tiempo. Reemplazan todos los Toast y snackbars.
- Encabezado único `EncabezadoNexo`: misma forma y tipografía en todas las pantallas, incluidas las de inicio de docente y estudiante. Solo cambia el color (`TonoEncabezado`).

### Verificación
- Backend: compila con kotlinc y pasan **40 pruebas JUnit**. Nuevas pruebas:
  - lotes de 30 y reintentos;
  - estadísticas;
  - Excel con marca y gráfico, que se abrió y se revisó visualmente.
- App: todo el código compila con kotlinc y las librerías del proyecto.

## Fase 6: errores críticos y experiencia del docente

### "No estás inscrito" al entrar al quiz (crítico)
- **Causa:** la app guardaba el token de sesión en caché 50 minutos sin fijarse de qué cuenta era. Al cerrar sesión como docente y entrar como estudiante (o al revés), la app seguía enviando el token de la cuenta anterior, y el servidor respondía que esa cuenta no estaba inscrita.
- **Corrección en la app:**
  - el token queda atado al UID de la sesión y se descarta si cambia la cuenta;
  - si el servidor responde 401, se pide un token nuevo y se reintenta una vez;
  - al cerrar sesión se limpia todo (token, `SessionManager` y preferencias).
- **Corrección en el backend:**
  - acepta inscripciones antiguas guardadas con otra clave (busca el UID en los campos `id`, `userId`, `uid` y `estudianteId`);
  - el mensaje de error ahora explica qué hacer y avisa si se está usando la cuenta del docente.

### Notificaciones
- Las bandejas leen la cuenta activa (UID de Firebase), no un UID guardado que podía ser de otra cuenta.
- Campana con contador de no leídas en el panel del docente y en el del estudiante. Si llega un aviso con la app abierta, se muestra en pantalla.
- Avisos nuevos desde el backend (`services/Notificador.kt`):
  - **docente:** nueva solicitud de inscripción, preguntas de IA listas para revisar y estudiante que aprobó un tema o el curso;
  - **estudiante:** solicitud rechazada (con el motivo) y nuevo contenido disponible cuando se aprueba la explicación de un tema.

### Pantallas rediseñadas (sin ventanas emergentes)
- **Gestión de grupos → curso:**
  - aspirantes e inscritos en la misma pantalla;
  - "Aceptar" es inmediato y hay "Aceptar a todos";
  - "Rechazar" abre el motivo dentro de la misma tarjeta;
  - solo aparecen los cursos del docente.
- **Validar preguntas:**
  - tarjetas para elegir el tema, con lo que falta por revisar;
  - botón **Aprobar todas** con confirmación, para todo el curso o solo un tema (`POST /api/preguntas/aprobar-pendientes`, una sola escritura);
  - las tarjetas de curso se rediseñaron.
- **Mis cursos → detalle del curso:** pantalla completa con datos, vigencia, temas con el estado de su explicación y accesos directos a estudiantes, validar, explicaciones y estadísticas. Editar también es pantalla completa.
- **Preguntas revisadas:**
  - filtros siempre visibles: estado, curso, tema, dificultad y búsqueda;
  - el detalle se abre en pantalla completa, con aprobar o rechazar y eliminar.
- **Pendientes del docente** (nueva, `GET /api/solicitudes/pendientes-docente`): muestra por tipo y por curso cuántas preguntas, explicaciones, temas sin explicación y aspirantes faltan, con acceso directo a cada pantalla. La tarjeta "Pendientes" del panel muestra el total.
- **Estadísticas:** cada gráfico se puede tocar para verlo en grande, con sus datos en lista.
- **Ranking docente:**
  - tarjetas nuevas con puesto (medalla para el top 3), avatar y XP, racha y vidas, resaltando la métrica elegida;
  - el perfil del estudiante es pantalla completa y suma promedio, quizzes, temas aprobados, actividad y si necesita apoyo.
- Los botones secundarios ya no tienen borde gris.

### Configuración
- **Ayuda y soporte:**
  - guía por rol (estudiante o docente) con preguntas frecuentes;
  - botón para escribir a soporte (brayner.trochez.o@uniautonoma.edu.co); el correo incluye versión, rol y dispositivo.
- **Acerca de EduRacha:**
  - qué es y cómo funciona;
  - creadores: Laura Valentina Solarte Muñoz y Brayner Stiven Trochez Ordóñez;
  - programa e institución, y tecnología.

### Verificación
- Backend: compila y pasan **43 pruebas JUnit**. Nuevas pruebas: centro de pendientes y formato de las notificaciones.
- App: todo el código compila con kotlinc y las librerías del proyecto. Falta probarla en el emulador.
