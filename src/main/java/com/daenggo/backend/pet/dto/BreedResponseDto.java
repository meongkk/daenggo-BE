package com.daenggo.backend.pet.dto;

import com.daenggo.backend.pet.entity.Breed;

/**
 * 견종 목록 조회 응답
 *
 * @param breedId 견종 ID
 * @param breedName 화면에 표시할 견종명
 */
public record BreedResponseDto(Long breedId, String breedName, boolean dangerous) {

    public static BreedResponseDto from(final Breed breed) {
        return new BreedResponseDto(breed.getId(), breed.getName(), breed.isDangerous());
    }
}
