package com.komik.komik.service.api.model

import com.google.gson.annotations.SerializedName

data class DoujindesuEncrypt(

	@field:SerializedName("_enc_resp_")
	val encResp: String? = null
)
