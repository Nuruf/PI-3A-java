package entities.annonce;

public class EventImage {
    private int idImage;
    private int postId;
    private String imagePath;
    private int ordre;

    public EventImage() {}

    public EventImage(int postId, String imagePath, int ordre) {
        this.postId = postId;
        this.imagePath = imagePath;
        this.ordre = ordre;
    }

    public int getIdImage() { return idImage; }
    public void setIdImage(int idImage) { this.idImage = idImage; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
}