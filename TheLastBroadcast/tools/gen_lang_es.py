#!/usr/bin/env python3
"""Creates the Spanish translation (es_es / es_mx) from en_us.json.
Run from the project root: python3 tools/gen_lang_es.py"""
import json

SRC = "src/main/resources/assets/lastbroadcast/lang/en_us.json"
OUT = "src/main/resources/assets/lastbroadcast/lang/"

ES = {
    "block.lastbroadcast.television": "Televisor de consola",
    "block.lastbroadcast.signal_analyzer": "Analizador de señal de vídeo",
    "block.lastbroadcast.tape_recorder": "Magnetófono de carrete abierto",
    "block.lastbroadcast.case_board": "Tablero de pruebas",
    "block.lastbroadcast.file_cabinet": "Archivador",
    "block.lastbroadcast.tape_shelf": "Estantería de la biblioteca de cintas",
    "block.lastbroadcast.notice_board": "Tablón de anuncios",
    "block.lastbroadcast.engineering_locker": "Taquilla de ingeniería",
    "block.lastbroadcast.skyhook_console": "Consola de sobremarcha Skyhook",
    "block.lastbroadcast.telephone": "Teléfono de sobremesa",
    "block.lastbroadcast.wall_clock": "Reloj de pared atómico",
    "block.lastbroadcast.ceiling_light": "Panel fluorescente de techo",
    "block.lastbroadcast.studio_light": "Foco Fresnel de estudio",
    "block.lastbroadcast.googie_lamp": "Lámpara de pie Sputnik",
    "block.lastbroadcast.on_air_sign": "Letrero EN EL AIRE",
    "block.lastbroadcast.tower_beacon": "Baliza de la torre",
    "block.lastbroadcast.tube_bank": "Banco de válvulas de vacío",
    "block.lastbroadcast.transmitter_coil": "Bobina del transmisor",
    "block.lastbroadcast.studio_camera": "Cámara de estudio",
    "block.lastbroadcast.control_panel": "Panel de control",
    "block.lastbroadcast.control_desk": "Mesa de control principal",
    "block.lastbroadcast.lounge_chair": "Sillón de salón",
    "block.lastbroadcast.terrazzo": "Terrazo",
    "block.lastbroadcast.studio_tile": "Baldosa acústica de estudio",
    "block.lastbroadcast.atomic_wallpaper": "Papel pintado atómico",
    "block.lastbroadcast.chrome_trim": "Moldura cromada",
    "block.lastbroadcast.walnut_panel": "Paneles de nogal",
    "block.lastbroadcast.googie_facade": "Fachada Googie",
    "item.lastbroadcast.magnetic_tape": "Carrete de cinta magnética",
    "item.lastbroadcast.magnetic_tape.recorded": "Cinta grabada",
    "item.lastbroadcast.document": "Documento",
    "item.lastbroadcast.case_journal": "Expediente de la FCC",
    "item.lastbroadcast.field_meter": "Medidor de intensidad de campo",
    "entity.lastbroadcast.staff": "Personal de WKVR",
    "itemGroup.lastbroadcast": "La última emisión",
    "subtitles.lastbroadcast.tv_on": "El televisor se enciende",
    "subtitles.lastbroadcast.tv_off": "El televisor se apaga",
    "subtitles.lastbroadcast.tv_static": "Nieve en la pantalla",
    "subtitles.lastbroadcast.channel_click": "El selector de canal hace clic",
    "subtitles.lastbroadcast.tape_insert": "Cinta enhebrada",
    "subtitles.lastbroadcast.tape_eject": "Cinta expulsada",
    "subtitles.lastbroadcast.tape_run": "El mecanismo de la cinta funciona",
    "subtitles.lastbroadcast.tape_rec": "El magnetófono empieza a grabar",
    "subtitles.lastbroadcast.tape_stop": "La cinta se detiene",
    "subtitles.lastbroadcast.tape_rewind": "La cinta retrocede",
    "subtitles.lastbroadcast.relay": "Un relé hace clic",
    "subtitles.lastbroadcast.breaker": "Interruptor accionado",
    "subtitles.lastbroadcast.valve": "Una válvula gira",
    "subtitles.lastbroadcast.arc": "Arco eléctrico",
    "subtitles.lastbroadcast.alarm": "Campana de alarma",
    "subtitles.lastbroadcast.power_down": "El transmisor se apaga",
    "subtitles.lastbroadcast.phone_ring": "Suena el teléfono",
    "subtitles.lastbroadcast.phone_pickup": "Se descuelga el auricular",
    "subtitles.lastbroadcast.dial_tone": "Tono de marcar",
    "subtitles.lastbroadcast.transmitter_hum": "La etapa de sobremarcha zumba",
    "subtitles.lastbroadcast.anomaly_whisper": "Algo susurra",
    "subtitles.lastbroadcast.anomaly_tone": "Suena un tono puro",
    "subtitles.lastbroadcast.electric_buzz": "Las luces zumban",
    "subtitles.lastbroadcast.deduction": "Se establece una conexión",
    "subtitles.lastbroadcast.evidence": "Prueba registrada",
    "subtitles.lastbroadcast.locker": "Pestillo de la taquilla",
    "subtitles.lastbroadcast.denied": "La cerradura no cede",
    "subtitles.lastbroadcast.static_burst": "Ráfaga de estática",
    "subtitles.lastbroadcast.fire_crackle": "El fuego crepita",
    "subtitles.lastbroadcast.jingle_wkvr": "Sintonía de WKVR",
    "subtitles.lastbroadcast.jingle_futura": "Sintonía de Futura",
    "subtitles.lastbroadcast.music_morning": "Música del programa matinal",
    "subtitles.lastbroadcast.music_news": "Fanfarria de noticias",
    "subtitles.lastbroadcast.music_contest": "Órgano del concurso",
    "subtitles.lastbroadcast.music_signoff": "Música de cierre",
    "subtitles.lastbroadcast.music_bulletin": "Drone tenso",
    "subtitles.lastbroadcast.test_tone": "Tono de prueba",
    "subtitles.lastbroadcast.applause": "Aplausos del estudio",
    "subtitles.lastbroadcast.voice_dot": "Dot habla",
    "subtitles.lastbroadcast.voice_hal": "El locutor habla",
    "subtitles.lastbroadcast.voice_penrose": "Penrose habla",
    "subtitles.lastbroadcast.voice_rosa": "Rosa habla",
    "subtitles.lastbroadcast.voice_brandt": "Brandt habla",
    "subtitles.lastbroadcast.voice_servo": "El robot habla",
    "subtitles.lastbroadcast.voice_reverse": "Voz al revés",
}

en = json.load(open(SRC, encoding="utf-8"))
missing = [k for k in en if k not in ES]
unknown = [k for k in ES if k not in en]
assert not unknown, unknown
out = {k: ES.get(k, v) for k, v in en.items()}
for name in ("es_es", "es_mx"):
    with open(OUT + name + ".json", "w", encoding="utf-8") as f:
        json.dump(out, f, indent=1, ensure_ascii=False)
        f.write("\n")
print("translated:", len(en) - len(missing), "of", len(en), "| untranslated (English kept):", missing)
