package com.daenggo.backend.pet.config;

import com.daenggo.backend.pet.entity.Breed;
import com.daenggo.backend.pet.repository.BreedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 서버 시작 시 펫 등록에 필요한 기본 견종 데이터를 준비한다.
 *
 * <p>이미 같은 이름의 견종이 있으면 유지하고, 없는 견종만 추가한다.</p>
 */
@Component
@RequiredArgsConstructor
public class BreedDataInitializer implements ApplicationRunner {

    private static final List<BreedSeed> DEFAULT_BREEDS = List.of(
            new BreedSeed("골든 리트리버", false),
            new BreedSeed("그레이트 데인", false),
            new BreedSeed("닥스훈트", false),
            new BreedSeed("도베르만 핀셔", false),
            new BreedSeed("도사견", true),
            new BreedSeed("래브라도 리트리버", false),
            new BreedSeed("로트와일러", true),
            new BreedSeed("말티즈", false),
            new BreedSeed("말티푸", false),
            new BreedSeed("미니어처 슈나우저", false),
            new BreedSeed("미니어처 핀셔", false),
            new BreedSeed("믹스견", false),
            new BreedSeed("바셋 하운드", false),
            new BreedSeed("버니즈 마운틴 도그", false),
            new BreedSeed("보더 콜리", false),
            new BreedSeed("보스턴 테리어", false),
            new BreedSeed("비글", false),
            new BreedSeed("비숑 프리제", false),
            new BreedSeed("사모예드", false),
            new BreedSeed("삽살개", false),
            new BreedSeed("셰틀랜드 쉽독", false),
            new BreedSeed("스태퍼드셔 불 테리어", true),
            new BreedSeed("시바 이누", false),
            new BreedSeed("시베리안 허스키", false),
            new BreedSeed("시추", false),
            new BreedSeed("아메리칸 스태퍼드셔 테리어", true),
            new BreedSeed("아메리칸 핏불 테리어", true),
            new BreedSeed("아키타", false),
            new BreedSeed("요크셔 테리어", false),
            new BreedSeed("웰시 코기", false),
            new BreedSeed("이탈리안 그레이하운드", false),
            new BreedSeed("잉글리시 불도그", false),
            new BreedSeed("재패니즈 스피츠", false),
            new BreedSeed("저먼 셰퍼드", false),
            new BreedSeed("진돗개", false),
            new BreedSeed("차우차우", false),
            new BreedSeed("치와와", false),
            new BreedSeed("카네 코르소", false),
            new BreedSeed("캐벌리어 킹 찰스 스패니얼", false),
            new BreedSeed("코커 스패니얼", false),
            new BreedSeed("파피용", false),
            new BreedSeed("퍼그", false),
            new BreedSeed("페키니즈", false),
            new BreedSeed("포메라니안", false),
            new BreedSeed("푸들", false),
            new BreedSeed("프렌치 불도그", false)
    );

    private final BreedRepository breedRepository;

    @Override
    @Transactional
    public void run(final ApplicationArguments args) {
        final Set<String> existingNames = breedRepository.findAll()
                .stream()
                .map(Breed::getName)
                .collect(Collectors.toSet());

        final List<Breed> missingBreeds = DEFAULT_BREEDS.stream()
                .filter(seed -> !existingNames.contains(seed.name()))
                .map(seed -> Breed.builder()
                        .name(seed.name())
                        .dangerous(seed.dangerous())
                        .build())
                .toList();

        if (!missingBreeds.isEmpty()) {
            breedRepository.saveAll(missingBreeds);
        }
    }

    /**
     * 초기 견종의 이름과 맹견 여부
     */
    private record BreedSeed(String name, boolean dangerous) {
    }
}
