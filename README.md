# Luma 1.0.0

**Tu galería, sin ruido.** App Android nativa de HecAgus, sin anuncios, cuentas, compras ni suscripciones. No declara permiso de Internet y no incluye bibliotecas de publicidad o analítica.

## Descargar

- [APK Luma 1.0.0](https://github.com/hecagus/luma-galeria/releases/download/v1.0.0/Luma-1.0.0.apk)
- [Versiones y cambios](https://github.com/hecagus/luma-galeria/releases)

## Instalar

1. Descarga `Luma-1.0.0.apk` en el teléfono y abre el archivo.
2. Si Android lo solicita, permite que esa aplicación abra instaladores y confirma la instalación de Luma.
3. Abre Luma y toca **Elegir acceso a fotos**. En Android 14 o posterior puedes permitir todas las fotos o seleccionar algunas.
4. Para ampliar el acceso después, usa **Opciones → Cambiar acceso a fotos**. Si Android ya no muestra la solicitud, usa **Opciones → Permisos de Android**.

Requiere Android 10 o posterior (API 29). La APK es universal: no contiene código nativo específico de procesador. Está dirigida a API 36 y firmada con una clave propia. No está publicada en Google Play.

## Funciones

- Cuadrícula de fotos locales con miniaturas y tres o cuatro columnas.
- Álbumes según las carpetas que Android incluye en su índice de imágenes; distingue volúmenes internos y tarjetas SD.
- Favoritos locales, sin modificar ni duplicar el archivo original.
- Búsqueda por nombre de archivo, álbum y fecha, sin distinguir mayúsculas ni acentos. No es una búsqueda por contenido visual.
- Orden por fecha reciente, fecha antigua o nombre de archivo.
- Visor con pellizco, doble toque, desplazamiento y botones anterior/siguiente.
- Compartir mediante el selector de Android; el destino lo elige el usuario.
- Detalles: nombre, fecha, carpeta, formato, resolución y tamaño.
- En Android 11 o posterior, envío a la papelera mediante una solicitud de confirmación de Android. La restauración se gestiona desde una aplicación que permita consultar la papelera del sistema; esta versión no incorpora su propio administrador de papelera.
- Acceso completo, parcial o denegado; al revocar acceso se vacía la vista y se cierra el visor.

La versión 1 se centra en fotos. Los archivos ocultos, carpetas excluidas por `.nomedia` y contenido que Android no indexa no aparecen automáticamente. Los formatos disponibles dependen de los decodificadores del teléfono. Las animaciones GIF se muestran como imagen fija.

## Privacidad

El manifiesto solo declara `READ_EXTERNAL_STORAGE` hasta API 32, `READ_MEDIA_IMAGES` y `READ_MEDIA_VISUAL_USER_SELECTED`. No pide ubicación, cámara, micrófono, notificaciones, acceso total a archivos ni permiso de Internet.

Las miniaturas se guardan temporalmente en memoria. Los favoritos y preferencias se guardan en el almacenamiento privado de la app. Luma no tiene nube ni servidor. Al compartir, la app que elijas recibirá la foto que seleccionaste. Eliminar Luma no elimina tus fotos originales.

## Compilación reproducible en Linux

Necesitas Python 3.10 o posterior y Java 21 con `java` y `keytool` disponibles. El proyecto de Android Studio se incluye como alternativa; la ruta siguiente fue la utilizada para producir la APK entregada.

```sh
python3 scripts/prepare_toolchain.py
python3 scripts/build.py
```

La preparación descarga paquetes fijados de Google (SDK 36, Build Tools 36.0.0) y Eclipse ECJ 3.43.0 desde Maven Central y verifica sus sumas. No instala nada en el teléfono. El build compila recursos, Java y DEX, alinea y firma `output/Luma-1.0.0.apk`. Verifica la firma, el nivel de Android, los permisos y la ausencia de bibliotecas nativas; genera `SHA256SUMS.txt`.

También puedes indicar rutas propias mediante `LUMA_ANDROID_JAR`, `LUMA_BUILD_TOOLS` y `LUMA_ECJ_JAR`.

## Clave para actualizaciones

**El repositorio público no contiene la clave de firma ni su contraseña.** El respaldo del propietario se conserva por separado. Para actualizar la APK publicada se necesita la misma clave; compilar desde el código con otra clave produce una instalación que Android no aceptará como actualización de la original.

La contraseña no aparece en los registros ni se pasa directamente como argumento. `.gitignore` excluye la carpeta de firma, las herramientas y las salidas. Para una actualización, conserva la firma e incrementa `versionCode` y `versionName` en los archivos de configuración; ajusta el nombre de salida del script.

## Pruebas

```sh
java -jar .toolchain/ecj.jar -1.8 -warn:none -d build/logic-tests app/src/main/java/mx/hecagus/luma/GalleryLogic.java tests/GalleryLogicTest.java
java -cp build/logic-tests mx.hecagus.luma.GalleryLogicTest
```

Las pruebas de interfaz y ejecución usan Robolectric 4.17 y JUnit 4.13.2, exclusivamente durante el desarrollo. Esas bibliotecas no forman parte de la APK. Con Maven instalado, resuelve las dependencias en `.toolchain/test-libs`:

```sh
mvn -B -f tests/runtime-pom.xml org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies -DoutputDirectory="$(pwd)/.toolchain/test-libs"
python3 tests/run_runtime.py
```

Las imágenes de `tests/fixtures` son patrones sintéticos para las pruebas. No se incluyen en la APK. Las pruebas no equivalen a ejecutar la app en un teléfono físico; sigue pendiente la validación en el dispositivo del usuario.

## Estructura

- `MainActivity`: navegación, permisos y acciones del usuario.
- `MediaRepository`: lectura de MediaStore y metadatos.
- `ThumbnailLoader`: caché acotada, carga asíncrona y reciclaje de vistas.
- `PhotoDecoder`: decodificación acotada, alternativa por streams y orientación EXIF.
- `ZoomImageView`: zoom, desplazamiento y navegación por gestos.
- `GalleryLogic`: búsqueda, fechas, tamaños y límites de decodificación.

Referencias de Android: [MediaStore](https://developer.android.com/training/data-storage/shared/media), [acceso parcial](https://developer.android.com/about/versions/14/changes/partial-photo-video-access), [AAPT2](https://developer.android.com/tools/aapt2) y [apksigner](https://developer.android.com/tools/apksigner).

## Página de descarga

`site/` contiene la página estática, las capturas y los metadatos públicos de la versión. Se publica en Vercel con `npm run build`; no requiere dependencias de npm ni acceso a la firma Android. El botón descarga el archivo publicado en GitHub Releases. La app Android continúa funcionando sin Internet.
