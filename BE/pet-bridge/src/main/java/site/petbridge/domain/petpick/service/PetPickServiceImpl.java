package site.petbridge.domain.petpick.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import site.petbridge.domain.follow.repository.FollowRepository;
import site.petbridge.domain.follow.service.FollowService;
import site.petbridge.domain.petpick.domain.PetPick;
import site.petbridge.domain.petpick.dto.request.PetPickEditRequestDto;
import site.petbridge.domain.petpick.dto.request.PetPickRegistRequestDto;
import site.petbridge.domain.petpick.dto.response.PetPickResponseDto;
import site.petbridge.domain.petpick.repository.PetPickRepository;
import site.petbridge.domain.petpickcomment.dto.response.PetPickCommentResponseDto;
import site.petbridge.domain.petpickcomment.repository.PetPickCommentRepository;
import site.petbridge.domain.petpicklike.repository.PetPickLikeRepository;
import site.petbridge.domain.user.domain.User;
import site.petbridge.domain.user.dto.response.UserResponseDto;
import site.petbridge.domain.user.repository.UserRepository;
import site.petbridge.domain.user.service.UserService;
import site.petbridge.global.exception.ErrorCode;
import site.petbridge.global.exception.PetBridgeException;
import site.petbridge.global.jwt.service.JwtService;
import site.petbridge.util.FileUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetPickServiceImpl implements PetPickService {

    private final PetPickRepository petPickRepository;
    private final FileUtil fileUtil;
    private final UserService userService;
    private final PetPickLikeRepository petPickLikeRepository;
    private final PetPickCommentRepository petPickCommentRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    /**
     * 펫픽 등록
     */
    @Override
    @Transactional
    public void registPetPick(HttpServletRequest httpServletRequest, final PetPickRegistRequestDto petPickRegistRequestDto,
                    MultipartFile thumbnailFile, MultipartFile videoFile) throws Exception {

        // 미인증 처리
        UserResponseDto userResponseDto = userService.isValidTokenUser(httpServletRequest).orElse(null);
        if (userResponseDto == null) {
            throw new PetBridgeException(ErrorCode.UNAUTHORIZED);
        }

        String savedThumbnailFileName = null;
        String savedVideoFileName = null;

        if (thumbnailFile != null) {
            savedThumbnailFileName = fileUtil.saveFile(thumbnailFile, "petpick");
        }

        if (videoFile != null) {
            savedVideoFileName = fileUtil.saveFile(videoFile, "petpick");
        }

        PetPick entity = petPickRegistRequestDto.toEntity(userResponseDto.id(), savedThumbnailFileName, savedVideoFileName);
        petPickRepository.save(entity);
    }

    /**
     * 펫픽 랜덤 목록 조회
     */
    @Override
    public List<PetPickResponseDto> getRandomListPetPick(HttpServletRequest httpServletRequest,
                                                         int initCommentSize) throws Exception {
        // 로그인 여부 확인
        String accessToken = jwtService.extractAccessToken(httpServletRequest).orElse(null);
        String email = jwtService.extractEmail(accessToken).orElse(null);

        User user = userRepository.findByEmail(email).orElse(null);

        List<PetPick> petPicks = petPickRepository.findRandomPetPicks();

        return petPicks.stream().map(petPick -> {
            User petPickWriter = userRepository.findById(petPick.getUserId()).orElse(null);
            String petPickWriterNickname = petPickWriter.getNickname();
            String petPickWriterImage = petPickWriter.getImage();

            // 펫픽 좋아요, 팔로우 여부 확인
            boolean isLiking = false;
            boolean isFollowing = false;
            // 로그인시
            if (user != null) {
                int userId = user.getId();
                isLiking = petPickLikeRepository.existsByUserIdAndPetPickId(userId, petPick.getId());
                isFollowing = followRepository.existsByUserIdAndAnimalId(userId, petPick.getAnimalId());
            }

            Sort sort = Sort.by(Sort.Direction.DESC, "registTime");
            Pageable pageable = PageRequest.of(0, initCommentSize, sort); // 최신순 페이징 initCommentSize 개수만큼
            List<PetPickCommentResponseDto> comments = petPickCommentRepository.findByPetPickId((long) petPick.getId(), pageable).stream()
                    .collect(Collectors.toList());

            int likeCnt = petPickLikeRepository.countByPetPickId(petPick.getId());

            return new PetPickResponseDto(petPick, petPickWriterNickname, petPickWriterImage, likeCnt, isLiking, isFollowing, comments);
        }).collect(Collectors.toList());
    }

    /**
     * 내가 쓴 펫픽 목록 조회
     */
    @Override
    public List<PetPickResponseDto> getListMyPetPick(HttpServletRequest httpServletRequest, int page, int size,
                                                     int initCommentSize) throws Exception {

        // 미인증 처리
        UserResponseDto userResponseDto = userService.isValidTokenUser(httpServletRequest).orElse(null);
        if (userResponseDto == null) {
            throw new PetBridgeException(ErrorCode.UNAUTHORIZED);
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "registTime");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PetPick> petpicks = petPickRepository.findByUserId(userResponseDto.id(), pageable);

        return getListPetPickResponseDtoByConditions(petpicks.getContent(), userResponseDto, initCommentSize);
    }

    /**
     * 내가 좋아요한 펫픽 목록 조회
     */
    @Override
    public List<PetPickResponseDto> getListLikePetPick(HttpServletRequest httpServletRequest, int page, int size, int initCommentSize) throws Exception {

        // 미인증 처리
        UserResponseDto userResponseDto = userService.isValidTokenUser(httpServletRequest).orElse(null);
        if (userResponseDto == null) {
            throw new PetBridgeException(ErrorCode.UNAUTHORIZED);
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "registTime");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PetPick> petpicks = petPickRepository.findLikedPetPicksByUserId(userResponseDto.id(), pageable);

        return getListPetPickResponseDtoByConditions(petpicks.getContent(), userResponseDto, initCommentSize);
    }

    /**
     * paging 된 내가 쓴 글, 좋아요한 글 petpick 가져온 후,
     * 작성자 정보 설정, 좋아요,팔로우 설정 및 댓글 페이징 처리 메소드
     */
    List<PetPickResponseDto> getListPetPickResponseDtoByConditions(List<PetPick> petPicks, UserResponseDto userResponseDto,
                                                                   int initCommentSize) {

        List<PetPickResponseDto> result = petPicks.stream().map(petPick -> {
            User petPickWriter = userRepository.findById(petPick.getUserId()).orElse(null);
            String petPickWriterNickname = petPickWriter.getNickname();
            String petPickWriterImage = petPickWriter.getImage();

            // 펫픽 좋아요, 팔로우 여부 확인
            boolean isLiking = false;
            boolean isFollowing = false;
            // 로그인 된 유저일 경우
            if (userResponseDto != null) {
                int userId = userResponseDto.id();
                isLiking = petPickLikeRepository.existsByUserIdAndPetPickId(userId, petPick.getId());
                isFollowing = followRepository.existsByUserIdAndAnimalId(userId, petPick.getAnimalId());
            }

            Sort sort = Sort.by(Sort.Direction.DESC, "registTime");
            Pageable pageable = PageRequest.of(0, initCommentSize, sort);
            List<PetPickCommentResponseDto> comments = petPickCommentRepository.findByPetPickId((long) petPick.getId(), pageable).stream()
                    .collect(Collectors.toList());

            int likeCnt = petPickLikeRepository.countByPetPickId(petPick.getId());

            return new PetPickResponseDto(petPick, petPickWriterNickname, petPickWriterImage, likeCnt, isLiking, isFollowing, comments);
        }).collect(Collectors.toList());

        return result;
    }

    /**
     *  펫핏 수정
     */
    @Override
    @Transactional
    public void editPetPick(HttpServletRequest httpServletRequest, PetPickEditRequestDto petPickEditRequestDto,
                            Long petPickId, MultipartFile thumbnailFile) throws Exception {

        // 미인증
        UserResponseDto userResponseDto = userService.isValidTokenUser(httpServletRequest).orElse(null);
        if (userResponseDto == null) {
            throw new PetBridgeException(ErrorCode.UNAUTHORIZED);
        }
        // 펫픽 없을 때
        PetPick entity = petPickRepository.findById(petPickId).orElseThrow(() -> new PetBridgeException(ErrorCode.RESOURCES_NOT_FOUND));
        // 내가 작성한 펫픽이 아닐 때
        if (userResponseDto.id() != entity.getUserId()) {
            throw new PetBridgeException(ErrorCode.FORBIDDEN);
        }
        // 조회했는데 삭제된 펫픽일 때
        if (entity.isDisabled()) {
            throw new PetBridgeException(ErrorCode.RESOURCES_NOT_FOUND);
        }

        String savedThumbnailFileName = null;
        if (thumbnailFile != null) {
            savedThumbnailFileName = fileUtil.saveFile(thumbnailFile, "petpick");
        }

        entity.update(petPickEditRequestDto.getBoardId(),
                petPickEditRequestDto.getTitle(),
                savedThumbnailFileName,
                petPickEditRequestDto.getContent());
    }

    /**
     * 펫픽 삭제
     */
    @Transactional
    @Override
    public void delete(HttpServletRequest httpServletRequest, final Long petPickId) throws Exception {

        // 미인증
        UserResponseDto userResponseDto = userService.isValidTokenUser(httpServletRequest).orElse(null);
        if (userResponseDto == null) {
            throw new PetBridgeException(ErrorCode.UNAUTHORIZED);
        }

        // 펫픽 없을 때
        PetPick entity = petPickRepository.findById(petPickId).orElseThrow(() -> new PetBridgeException(ErrorCode.RESOURCES_NOT_FOUND));

        // 내가 작성한 쇼츠가 아닐 때
        if (userResponseDto.id() != entity.getUserId()) {
            throw new PetBridgeException(ErrorCode.FORBIDDEN);
        }

        // 이미 삭제된 쇼츠일 때
        if (entity.isDisabled()) {
            throw new PetBridgeException(ErrorCode.CONFLICT);
        }

        entity.disable();
    }


}
