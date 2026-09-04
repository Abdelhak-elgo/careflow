package com.elgourmat.careflow.application.port.out;

import java.util.Map;

public interface AuditPort {

    void record(String action, String entityType, String entityId, Map<String, Object> details);
}
