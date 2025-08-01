package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.entity.CodeSequence;
import vn.edu.fpt.medicaldiagnosis.repository.CodeSequenceRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class CodeGeneratorService {

    private final CodeSequenceRepository codeSequenceRepository;

    public String generateCode(String codeType, String prefix, int padLength) {
        CodeSequence sequence = codeSequenceRepository.findByCodeTypeForUpdate(codeType)
                .orElseGet(() -> new CodeSequence(codeType, 0L));

        Long nextValue = sequence.getCurrentValue() + 1;
        sequence.setCurrentValue(nextValue);
        codeSequenceRepository.save(sequence);

        String padded = String.format("%0" + padLength + "d", nextValue);
        return prefix + padded;
    }
}
