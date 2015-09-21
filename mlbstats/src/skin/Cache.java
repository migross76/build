package skin;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import data.Master;
import data.War;

public class Cache {
  public BufferedImage fetch(String playerID) throws IOException {
    File f = new File(_dir + "/" + playerID + ".jpg");
    if (f.exists()) { return ImageIO.read(f); }
    // else; fetch and save file
    Master m = _mt.byID(playerID);
    int yr = _id.get(playerID).first().yearID();
    if (yr < 2000) { yr -= 1900; }
    String url = "http://seamheads.com/baseballgauge/pics/" + yr + "/" + m.nameFirst().toLowerCase() + "_" + m.nameLast().toLowerCase() + ".jpg";
    BufferedImage image = ImageIO.read(new URL(url));
    ImageIO.write(image, "jpg", f);
    return image;
  }

  public Cache(String dir, Master.Table mt, War.Table wt) {
    _dir = dir;
    _mt = mt;
    _id = new War.ByID();
    _id.addAll(wt);
  }

  private final String _dir;
  private final Master.Table _mt;
  private final War.ByID _id;
}
