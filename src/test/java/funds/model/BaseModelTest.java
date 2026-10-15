package funds.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BaseModelTest {

    @Test
    void testNoArgsConstructor_DefaultsAllFieldsToNull() {
        BaseModel model = new BaseModel();

        assertNull(model.getCreatedAt());
        assertNull(model.getUpdatedAt());
        assertNull(model.getCreatedBy());
        assertNull(model.getUpdatedBy());
    }

    @Test
    void testSettersAndGetters_RoundTrip() {
        BaseModel model = new BaseModel();
        LocalDateTime now = LocalDateTime.of(2025, 1, 15, 10, 30);

        model.setCreatedAt(now);
        model.setUpdatedAt(now.plusHours(1));
        model.setCreatedBy("TEST");
        model.setUpdatedBy("TEST_UPDATED");

        assertEquals(now, model.getCreatedAt());
        assertEquals(now.plusHours(1), model.getUpdatedAt());
        assertEquals("TEST", model.getCreatedBy());
        assertEquals("TEST_UPDATED", model.getUpdatedBy());
    }

    @Test
    void testEqualsAndHashCode_WithSameFieldValues_AreEqual() {
        BaseModel first = new BaseModel();
        first.setCreatedBy("TEST");
        BaseModel second = new BaseModel();
        second.setCreatedBy("TEST");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
