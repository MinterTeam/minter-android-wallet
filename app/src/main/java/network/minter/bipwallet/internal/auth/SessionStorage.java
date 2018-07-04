package network.minter.bipwallet.internal.auth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;

import timber.log.Timber;


/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class SessionStorage {
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private Context mContext;

    @SuppressLint("CommitPrefEdits")
    public SessionStorage(Context context, GsonBuilder gsonBuilder) {
        mPrefs = context.getSharedPreferences(context.getApplicationInfo().name, Context.MODE_PRIVATE);
        editor = mPrefs.edit();
        gson = gsonBuilder.create();
        mContext = context;
    }

    public void remove(String key) {
        editor.remove(key);
        editor.commit();
    }

    /**
     * Пример:
     * String myStringValue = get("myKey", String.class)
     * MyObject myObjValue = get("myObjKey", MyObject.class)
     *
     * Класс нужен для того чтобы GSON мог десериализовать данные
     *
     * @param key
     * @param type
     * @param <T>
     * @return
     */
    @Nullable
    public <T> T get(String key, Class<T> type) {
        String json = mPrefs.getString(key, null);
        if (json == null) {
            return null;
        }

        return gson.fromJson(json, type);
    }

    @Nullable
    public <T> T get(String key, Type type) {
        String json = mPrefs.getString(key, null);
        if (json == null) {
            return null;
        }

        return gson.fromJson(json, type);
    }

    public void set(String key, Object object) {
        editor.putString(key, gson.toJson(object, object.getClass()));
        editor.commit();
    }

    public boolean has(String key) {
        return mPrefs.contains(key);
    }

    public boolean deleteImage(Context context, String key) throws RuntimeException {
        File myDir = getApplicationDirectory(context, "saved_images");
        String fileName = key + ".jpg";

        File file = new File(myDir, fileName);
        boolean deleted = false;
        if (file.exists()) {
            deleted = file.delete();
        }

        return deleted;
    }

    public void saveImage(Bitmap image) {
        String fileName = AuthSession.AVATAR_RESTORATION_KEY;
        File myDir = getApplicationDirectory(mContext, "saved_images");

        if (!myDir.canWrite()) {
            Timber.e("Cant't write image to " + myDir.getAbsolutePath());
            return;
        }

        String fileNameWithExt = fileName + ".jpg";
        //noinspection ResultOfMethodCallIgnored
        myDir.mkdirs();
        File file = new File(myDir, fileNameWithExt);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }

        try {
            //noinspection ResultOfMethodCallIgnored если файл есть и по какой-то причине не удалился, то запишем данные в начало текущего файла
            file.createNewFile();
            FileOutputStream os = new FileOutputStream(file, false);
            image.compress(Bitmap.CompressFormat.JPEG, 80, os);
            os.flush();
            os.close();
            set(fileName, file.toString());
        } catch (Exception e) {
            Timber.w(e, "Error while saving avatar");
        }
    }

    public File getAvatarPath() {
        String path = get(AuthSession.AVATAR_RESTORATION_KEY, String.class);
        if (path == null) {
            return null;
        }

        return new File(path);
    }

    public boolean isAvatarExists() {
        String path = get(AuthSession.AVATAR_RESTORATION_KEY, String.class);
        if (path == null) {
            return false;
        }
        File file = new File(path);
        return file.exists();
    }

    public void replaceAvatar(Bitmap image) {
        saveImage(image);
    }

    public Bitmap getAvatarBitmap() {
        File file = getAvatarPath();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(file.getPath(), options);
    }

    public boolean hasAvatar() {
        return getAvatarPath() != null;
    }

    private File getApplicationDirectory(Context context, String subDir) {
        ContextWrapper cw = new ContextWrapper(context.getApplicationContext());
        File dir = cw.getDir(subDir, Context.MODE_PRIVATE);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }
}
