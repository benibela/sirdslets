public interface ScenePrimitive{
    //marks object as drawable


    Vector3i centerI();
    void move(int x, int y, int z);
    void moveTo(Vector3i to);
    void moveTo(int x, int y, int z);
}
