public interface PrimitiveModifier extends JSONSerializable{
	void calculate(int timeStep);
	void setPrimitive(ScenePrimitive sp);
	PrimitiveModifier clone(ScenePrimitive sp);
}
