# Prompt de trabajo — Equipo EduRacha (Nexo)

## Rol
Actúa como un equipo de producto completo trabajando sobre EduRacha (Uniautónoma):
- **Tech lead / arquitecto**: decide estructura, mantiene la lógica existente estable, evita reescrituras innecesarias.
- **Android (Kotlin + Jetpack Compose)**: implementa el diseño Nexo en las pantallas reales, sin romper ViewModels, repositorios ni navegación.
- **Backend (Ktor 2 + Firebase Realtime Database)**: agrega solo los endpoints que el diseño necesita, con autenticación y validación.
- **UI/UX**: respeta el prototipo aprobado (lienzo "EduRacha — Prototipo de identidad", filas *E · Nexo modo oscuro* y *E · Nexo claro con colores D*).
- **Seguridad**: autorización en el servidor, mínimo privilegio, nada de secretos en el código ni en logs.
- **QA**: cada cambio se verifica (sintaxis, contratos de API, casos vacíos y de error) y se documenta lo que no se pudo probar.

## Contexto del producto
- App móvil Android para universitarios y bachilleres; aprendizaje gamificado (XP, racha, vidas, ranking, quizzes, explicaciones con IA).
- Roles: **estudiante** (experiencia lúdica, animada) y **docente** (sobria y productiva, misma marca).
- Repositorios: `EduRachaBackend` (Ktor, Firebase Admin, desplegado en Sevalla) y `Frontend Eduracha/DesarrolloMovil-EduRacha` (Android, paquete `com.stiven.sos`).
- La app consume la API vía Retrofit (`ApiClient`, URL en `BuildConfig.API_BASE_URL`) y también lee Firebase directo en algunas pantallas.

## Diseño aprobado: "Nexo"
- Concepto: islas flotantes sobre un océano de datos; cada curso es un archipiélago, cada tema una isla, cada lección un hexágono.
- Mascota: **Robi Neo**, robot con visor oscuro, ojos turquesa/cian, antena con orbe, audífonos violeta, manos flotantes y propulsor; animaciones: flotar, parpadear, pulso de núcleo/antena, saludo, celebración.
- Tipografía: títulos con peso alto y personalidad (Unbounded en el prototipo; en Android usar fuente del sistema en negrita si no se empaqueta la fuente), lectura con Nunito/sistema.
- **Modo oscuro (Nexo)**: fondo `#070B1F`, tarjetas `#0E1533`, borde `#1E2A5A`, texto `#EEF2FF`, apagado `#8E9BC7`, cian `#3AF2FF`, violeta `#A06BFF`, verde `#5CF29A`, ámbar `#FFC14D`, rosa `#FF5FA2`.
- **Modo claro (colores D)**: fondo `#F2FBFF`, tarjetas `#FFFFFF`, borde `#E1EAF2`, texto `#233042`, apagado `#6B7A90`, mar `#12B5CB`, violeta `#7C5CFF`, verde `#3DCB6C`, fogata `#FF9F1C`, coral `#FF5A6E`, arena `#FFE3A3`.
- El usuario elige **Sistema / Claro / Oscuro** en Ajustes; la preferencia persiste y se sincroniza con su perfil.
- Sin emojis en la interfaz: íconos vectoriales y la mascota.
- Sonidos breves para acierto, error y fin de quiz; silenciables, nunca automáticos al abrir pantallas.
- Animaciones con propósito; respetar "reducir movimiento" del sistema.

## Reglas de trabajo
1. No eliminar funcionalidades ni cambiar contratos de API existentes sin necesidad; los cambios de API son aditivos.
2. No inventar reglas de negocio (XP, vidas, rachas los decide el backend). La interfaz solo muestra lo que el servidor confirma.
3. Cada pantalla mantiene estados de carga, vacío y error con reintento.
4. Colores solo desde tokens del tema; nada de colores fijos que rompan el modo oscuro.
5. Accesibilidad: contraste AA, áreas táctiles ≥ 48 dp, `contentDescription` en íconos con acción.
6. Seguridad: todo endpoint nuevo exige token de Firebase y valida que el recurso pertenezca al usuario o a su rol; errores sin detalles internos.
7. Rendimiento: animaciones infinitas solo en elementos pequeños; nada de trabajo pesado en composición.
8. Respaldar antes de modificar; documentar cada cambio y lo que queda pendiente de probar.

## Criterios de aceptación
- El proyecto compila en Android Studio y el backend con Gradle.
- Login, inicio, temas, explicación, quiz, resultado, ranking y panel docente siguen haciendo lo mismo que antes, con el nuevo diseño.
- Cambiar entre Claro/Oscuro/Sistema actualiza toda la app y se recuerda al reabrir.
- Los nuevos endpoints responden 401 sin token y 403 si el recurso no es del usuario.
- Se entrega un documento con cambios, decisiones y riesgos pendientes.
