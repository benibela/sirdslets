public interface ScenePrimitive extends JSONSerializable{
    //marks object as drawable


    Vector3i centerI();
    void move(int x, int y, int z);
    void moveTo(Vector3i to);
    void moveTo(int x, int y, int z);
    ScenePrimitive fastClone(); //O(1) copy != this, but possible with dependancies on this (e.g. shared height map)
}
