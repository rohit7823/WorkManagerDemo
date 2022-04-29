package com.example.workmanagerdemo.model

import kotlinx.serialization.Serializable


@Serializable
data class File(
	val name: String,
	val downloadLink: String,
	val fileType: String
)