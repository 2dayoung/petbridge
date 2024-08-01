package site.petbridge.domain.petpickcomment.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import site.petbridge.domain.petpick.dto.request.PetPickEditRequestDto;
import site.petbridge.domain.petpick.dto.response.PetPickResponseDto;
import site.petbridge.domain.petpickcomment.dto.request.PetPickCommentEditRequestDto;
import site.petbridge.domain.petpickcomment.dto.request.PetPickCommentRegistRequestDto;
import site.petbridge.domain.petpickcomment.dto.response.PetPickCommentResponseDto;

import java.util.List;

public interface PetPickCommentService {

    /**
     * 펫픽 댓글 등록
     */
    void registPetPickComment(HttpServletRequest httpServletRequest,
             final PetPickCommentRegistRequestDto petPickCommentRegistRequestDto) throws Exception;

    /**
     * 펫픽 댓글 목록 조회
     */
    List<PetPickCommentResponseDto> getListPetPickComment(Long petPickId, int page, int size);

    /**
     * 펫픽 댓글 수정
     */
    void editPetPickComment(HttpServletRequest httpServletRequest,
                            final Long petPickCommentId,
                            final PetPickCommentEditRequestDto petPickCommentEditRequestDto
                ) throws Exception;

    /**
     * 펫픽 댓글 삭제
     */
    void removePetPickComment(HttpServletRequest httpServletRequest, Long id) throws Exception;
}
