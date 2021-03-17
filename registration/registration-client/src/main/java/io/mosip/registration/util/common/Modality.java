package io.mosip.registration.util.common;

import io.mosip.registration.constants.RegistrationConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum Modality {
    FINGERPRINT_SLAB_LEFT(RegistrationConstants.leftHandUiAttributes),
    FINGERPRINT_SLAB_RIGHT(RegistrationConstants.rightHandUiAttributes),
    FINGERPRINT_SLAB_THUMBS(RegistrationConstants.twoThumbsUiAttributes),
    IRIS_DOUBLE(RegistrationConstants.eyesUiAttributes),
    FACE(RegistrationConstants.faceUiAttributes),
    EXCEPTION_PHOTO(Collections.EMPTY_LIST);

    public List<String> getAttributes() {
        return attributes;
    }

    private List<String> attributes;

    Modality(List<String> attributes) {
        this.attributes = attributes;
    }

    public static List<String> getAllBioAttributes() {
        List<String> allAttributes = new ArrayList<>();
        allAttributes.addAll(FINGERPRINT_SLAB_LEFT.attributes);
        allAttributes.addAll(FINGERPRINT_SLAB_RIGHT.attributes);
        allAttributes.addAll(FINGERPRINT_SLAB_THUMBS.attributes);
        allAttributes.addAll(IRIS_DOUBLE.attributes);
        allAttributes.addAll(FACE.attributes);
        return allAttributes;
    }

    public static List<String> getAllBioAttributes(Modality modality) {
        switch (modality) {
            case FINGERPRINT_SLAB_THUMBS:
                return FINGERPRINT_SLAB_THUMBS.attributes;
            case FINGERPRINT_SLAB_RIGHT:
                return FINGERPRINT_SLAB_RIGHT.attributes;
            case FINGERPRINT_SLAB_LEFT:
                return FINGERPRINT_SLAB_LEFT.attributes;
            case IRIS_DOUBLE:
                return IRIS_DOUBLE.attributes;
            case FACE:
                return FACE.attributes;
        }
        return Collections.EMPTY_LIST;
    }

}
