package com.game.tcg.persistence

import java.io.File

/**
 * 진행 상태 저장소 — 저장 문자열 한 덩어리를 읽고/쓰고/지운다.
 *
 * 실제 앱은 [FileSaveStore] (앱 내부 저장소 `context.filesDir`), 단위 테스트는
 * [InMemorySaveStore] 를 쓴다.
 */
interface SaveStore {
    /** 저장된 문자열 (없거나 읽을 수 없으면 null) */
    fun read(): String?

    /** 저장 (실패해도 예외를 던지지 않는다 — 게임 진행을 막지 않기 위해) */
    fun write(text: String)

    /** 저장 내용 삭제 (새 게임 시작) */
    fun clear()

    companion object {
        /** 앱 내부 저장소에 두는 세이브 파일 이름 */
        const val SAVE_FILE_NAME: String = "dual_gate_save.dat"
    }
}

/**
 * 앱 내부 저장소 파일에 저장한다. (JSON 형식 텍스트 · UTF-8)
 *
 * 임시 파일에 먼저 쓴 뒤 이름을 바꿔치기해, 저장 도중 앱이 죽어도 **반쯤 쓰인 파일**이 남지 않는다.
 */
class FileSaveStore(file: File) : SaveStore {

    private val saveFile: File = file
    private val tempFile: File = File(file.parentFile, file.name + ".tmp")

    override fun read(): String? = runCatching {
        if (saveFile.exists()) saveFile.readText(Charsets.UTF_8) else null
    }.getOrNull()

    override fun write(text: String) {
        runCatching {
            saveFile.parentFile?.mkdirs()
            tempFile.writeText(text, Charsets.UTF_8)
            if (!tempFile.renameTo(saveFile)) {
                // 이름 바꾸기 실패(플랫폼 차이) → 직접 덮어쓰고 임시 파일 정리
                saveFile.writeText(text, Charsets.UTF_8)
                tempFile.delete()
            }
        }
    }

    override fun clear() {
        runCatching {
            if (saveFile.exists()) saveFile.delete()
            if (tempFile.exists()) tempFile.delete()
        }
    }
}

/** 테스트·검증용 메모리 저장소 (앱이 꺼졌다 켜지는 상황은 같은 인스턴스를 넘겨 재현한다) */
class InMemorySaveStore(initial: String? = null) : SaveStore {

    var stored: String? = initial
        private set

    /** write/clear 가 몇 번 일어났는지 (불필요한 저장이 없는지 검증용) */
    var writeCount: Int = 0
        private set

    override fun read(): String? = stored

    override fun write(text: String) {
        stored = text
        writeCount += 1
    }

    override fun clear() {
        stored = null
    }
}
