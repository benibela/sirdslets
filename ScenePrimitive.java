public interface ScenePrimitive extends JSONSerializable{
    //marks object as drawable

    void drawTo(ZDraw map, int dx, int dy);
	void drawTo(ZDraw map, ZDraw colorMap, int dx, int dy);

	Vector3i centerI();
    void move(int x, int y, int z);
    void moveTo(Vector3i to);
    void moveTo(int x, int y, int z);
    Vector3i cornerLTF();
    Vector3i cornerRBN();
    int zAt(int x, int y);
    ScenePrimitive fastClone(); //O(1) copy != this, but possible with dependancies on this (e.g. shared height map)
}
