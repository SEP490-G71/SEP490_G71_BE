package vn.edu.fpt.medicaldiagnosis.config;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CallbackRegistry {

    private final Set<String> callbackPatientIds = ConcurrentHashMap.newKeySet();

    public void register(String patientId) {
        callbackPatientIds.add(patientId);
    }

    public boolean contains(String patientId) {
        return callbackPatientIds.contains(patientId);
    }

    public void remove(String patientId) {
        callbackPatientIds.remove(patientId);
    }
}
