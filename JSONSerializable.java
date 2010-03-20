
import java.util.Map;

public interface JSONSerializable {
	Map<String, Object> jsonSerialize();
	void jsonDeserialize(Map<String, Object> obj);
}
