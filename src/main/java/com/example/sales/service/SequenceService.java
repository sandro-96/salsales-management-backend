package com.example.sales.service;

public interface SequenceService {
    String getNextCode(String shopId, String prefix, String type);
    void updateNextSequence(String shopId, String prefix, String type);
}