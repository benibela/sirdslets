public interface PrimitiveModifier extends JSONSerializable{
	void calculate(double timeStep);
	void setPrimitive(ScenePrimitive sp);
	PrimitiveModifier clone(ScenePrimitive sp);
}
