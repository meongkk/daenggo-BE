package com.daenggo.backend.pet.service;

import com.daenggo.backend.pet.dto.BreedResponseDto;
import com.daenggo.backend.pet.repository.BreedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 견종 정보 조회 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BreedService {

    private static final String DIRECT_INPUT_BREED_NAME = "직접 입력";

    private final BreedRepository breedRepository;

    /**
     * 펫 등록 화면에서 선택할 수 있는 견종 목록 조회
     *
     * @return 이름순으로 정렬된 견종 목록
     */
    public List<BreedResponseDto> getBreeds() {
        return breedRepository
                .findAllByNameNotOrderByDangerousAscNameAsc(DIRECT_INPUT_BREED_NAME)
                .stream()
                .map(BreedResponseDto::from)
                .toList();
    }
}
