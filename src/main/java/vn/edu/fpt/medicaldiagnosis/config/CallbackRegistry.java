package vn.edu.fpt.medicaldiagnosis.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CallbackRegistry {
    private final Map<String, String> callbackMap = new ConcurrentHashMap<>();

    public void register(String patientId, String callbackUrl) {
        callbackMap.put(patientId, callbackUrl);
    }

    public Optional<String> get(String patientId) {
        return Optional.ofNullable(callbackMap.get(patientId));
    }

    public void remove(String patientId) {
        callbackMap.remove(patientId);
    }
}
