package io.mosip.registration.processor.packet.manager.dto;

public class CreateDraftV2RequestDto {

	private String uin;

	private boolean generateUin;

	public CreateDraftV2RequestDto(String uin, boolean generateUin) {
		this.uin = uin;
		this.generateUin = generateUin;
	}

	public String getUin() {
		return uin;
	}

	public boolean isGenerateUin() {
		return generateUin;
	}
}
