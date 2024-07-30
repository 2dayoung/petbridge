package site.petbridge.domain.board.dto.response;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardResponseDto {

	private int id;
	private int userId;
	private int animalId;
	private String type;
	private String thumbnail;
	private String title;
	private String content;
	private String lat;
	private String lon;
	private Timestamp registTime;
	private boolean disabled;

	private String userNickname;
	private String userImage;

	private String animalName;
	private String animalThumbnail;

	private int commentCount;

}
