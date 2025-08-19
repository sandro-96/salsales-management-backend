package com.example.sales.service.impl;

import com.example.sales.model.NextSequence;
import com.example.sales.repository.NextSequenceRepository;
import com.example.sales.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SequenceServiceImpl implements SequenceService {
    private final NextSequenceRepository nextSequenceRepository;

    @Override
    public String getNextCode(String shopId, String prefix, String type) {
        NextSequence nextSequence = nextSequenceRepository
                .findByShopIdAndPrefixAndType(shopId, prefix, type)
                .orElse(NextSequence.builder()
                        .shopId(shopId)
                        .prefix(prefix)
                        .type(type)
                        .nextSequence(1)
                        .build());
        return String.format("%s_%03d", prefix, nextSequence.getNextSequence());
    }

    @Override
    public void updateNextSequence(String shopId, String prefix, String type) {
        NextSequence nextSequence = nextSequenceRepository
                .findByShopIdAndPrefixAndType(shopId, prefix, type)
                .orElse(NextSequence.builder()
                        .shopId(shopId)
                        .prefix(prefix)
                        .type(type)
                        .nextSequence(1)
                        .build());
        nextSequence.setNextSequence(nextSequence.getNextSequence() + 1);
        nextSequenceRepository.save(nextSequence);
    }
}