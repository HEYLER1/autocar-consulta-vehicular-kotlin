# Fuentes de multas y papeletas

Retirados del directorio visible tras dos comprobaciones: Andahuaylas (HTTP 403) y Huancayo (sin conexión). Los enlaces quedan documentados aquí para posible recuperación. Arequipa respondió HTTP 200 con identificador de navegador móvil. Las demás fuentes respondieron HTTP 200; esto confirma respuesta HTTP, no una consulta completa ni compatibilidad de todos los captchas.

Revisión: 09/10/2026. Directorio de partida: https://www.peruautos.pe/consulta-vehicular-peru/

Este inventario organiza fuentes; no representa resultados de una placa ni confirma ausencia de deudas. El registro de una entidad no cubre necesariamente las demás.

## Nacional

| Entidad | Consulta | Acceso |
|---|---|---|
| SUTRAN | Récord de infracciones | https://www.sutran.gob.pe/consultas/record-de-infracciones/record-de-infracciones/ |

SUTRAN indica que las infracciones anteriores a 2020 requieren solicitud por Mesa de Partes.

## Lima y Callao

| Entidad | Consulta | Acceso | Observación |
|---|---|---|---|
| SAT Lima | Papeletas e infracciones | https://www.sat.gob.pe/Websitev8/modulos/contenidos/ConsultaPapeletasv3.aspx | Evitar enlaces de VirtualSAT con sesión incrustada |
| ATU | Actas de fiscalización de transporte urbano | https://pasarela.atu.gob.pe/ | Placa o número de acta; servicio confirmado por ATU en gob.pe |
| Municipalidad del Callao | Papeletas | https://pagopapeletascallao.pe/ | Placa y código de seguridad; vinculado por Municipalidad en gob.pe |

## Provincias

| Ciudad | Fuente | Estado de revisión |
|---|---|---|
| Arequipa | https://www.muniarequipa.gob.pe/oficina-virtual/c0nInfrPermisos/faltas/papeletas.php | Formulario de infracciones pendientes por placa |
| Cajamarca | https://www.satcajamarca.gob.pe/ | Portal institucional accesible |
| Chachapoyas | https://app.munichachapoyas.gob.pe/servicios/consulta_papeletas/app/papeletas.php | Consulta municipal accesible |
| Chiclayo | https://virtualsatch.satch.gob.pe/virtualsatch/record_infracciones/buscar_placa_ | Servicio de récord accesible |
| Cusco | https://cusco.gob.pe/informatica/index.php/ | Portal de servicios accesible |
| Ica | https://m.satica.gob.pe/ | Portal móvil accesible |
| Tacna | https://www.munitacna.gob.pe/pagina/sf/servicios/papeletas | Servicio institucional accesible |
| Tarapoto | https://www.sat-t.gob.pe/ | Portal institucional accesible |
| Trujillo | https://www.gob.pe/53428-verificar-informacion-de-papeletas-de-transito-y-transporte-en-trujillo | Preferir acceso institucional; el enlace del directorio mostró contenido ajeno |
| Piura | https://web.satp.gob.pe/servicios/consulta-en-linea | Acceso existente en la app; enlace del directorio no pudo verificarse |
| Huancayo | https://app1.munihuancayo.gob.pe/GTT/consulta | Acceso existente en la app; enlace externo del directorio requiere revalidación |
| Huánuco | https://www.munihuanuco.gob.pe/ | Referencia del directorio; consulta exacta pendiente de verificación |
| Pucallpa | https://consultas.municportillo.gob.pe/ | Referencia del directorio; consulta pendiente de verificación |
| Andahuaylas | https://muniandahuaylas.gob.pe/ | Referencia del directorio; consulta pendiente de verificación |

## Estructura para recopilar resultados

Cada infracción debe guardar entidad, ciudad, placa, número de papeleta o acta, código, fecha, importe, estado y fecha de consulta. Campos ausentes: no disponible. Consulta fallida: pendiente/no disponible, nunca cero multas.

No sumar una misma papeleta repetida: identificar por entidad y número de acta/papeleta. Separar pendientes, pagadas y estado desconocido. No mezclar impuesto vehicular con multas de tránsito.

## Referencias institucionales contrastadas

- ATU: https://www.gob.pe/institucion/atu/noticias/1175705-pasarela-atu-la-herramienta-digital-para-simplificar-pagos-y-consultas-en-linea
- Callao: https://www.gob.pe/institucion/municallao/noticias/810833-inicio-en-pago-de-papeletas-en-el-callao-desde-s-5-soles
- SUTRAN: https://www.sutran.gob.pe/consultas/record-de-infracciones/record-de-infracciones/
