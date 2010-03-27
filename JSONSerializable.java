public interface JSONSerializable {
	Object jsonSerialize();
	void jsonDeserialize(Object obj);
}
