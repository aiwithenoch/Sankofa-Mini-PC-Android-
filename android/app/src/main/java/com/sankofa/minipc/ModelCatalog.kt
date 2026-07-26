package com.sankofa.minipc

data class CatalogModel(
    val id: String,
    val displayName: String,
    val description: String,
    val downloadUrl: String,
    val fileName: String,
    val sha256: String,
    val minimumRamMb: Long,
    val license: String,
)

object ModelCatalog {
    val starter = CatalogModel(
        id = "qwen3-0.6b-q8",
        displayName = "Qwen3 0.6B Q8",
        description = "Small official GGUF used to prove the Android download and llama.cpp path.",
        downloadUrl = "https://huggingface.co/Qwen/Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B-Q8_0.gguf?download=true",
        fileName = "Qwen3-0.6B-Q8_0.gguf",
        sha256 = "9465e63a22add5354d9bb4b99e90117043c7124007664907259bd16d043bb031",
        minimumRamMb = 2_048,
        license = "Apache-2.0",
    )

    val all: List<CatalogModel> = listOf(starter)
}
