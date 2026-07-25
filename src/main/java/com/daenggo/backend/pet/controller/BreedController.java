package com.daenggo.backend.pet.controller;

import com.daenggo.backend.pet.dto.BreedResponseDto;
import com.daenggo.backend.pet.service.BreedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 견종 정보 REST 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/breeds")
public class BreedController {

    private final BreedService breedService;

    /**
     * 펫 등록 화면에서 사용할 견종 목록 조회
     *
     * @return 견종 ID와 이름 목록
     */
    @GetMapping
    public ResponseEntity<List<BreedResponseDto>> getBreeds() {
        return ResponseEntity.ok(breedService.getBreeds());
    }
}
