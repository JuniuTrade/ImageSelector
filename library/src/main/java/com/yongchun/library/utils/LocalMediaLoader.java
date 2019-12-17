package com.yongchun.library.utils;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.yongchun.library.model.LocalMedia;
import com.yongchun.library.model.LocalMediaFolder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import static com.yongchun.library.view.ImageSelectorActivity.TYPE_IMAGE;
import static com.yongchun.library.view.ImageSelectorActivity.TYPE_VIDEO;

/**
 * Created by dee on 15/11/19.
 */
public class LocalMediaLoader {

    private final static String[] IMAGE_PROJECTION = {
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media._ID};

    private final static String[] VIDEO_PROJECTION = {
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DURATION};

    private int type = TYPE_IMAGE;
    private FragmentActivity activity;

    public LocalMediaLoader(FragmentActivity activity, int type) {
        this.activity = activity;
        this.type = type;
    }

    HashSet<String> mDirPaths = new HashSet<String>();

    public void loadAllImage(final LocalMediaLoadListener imageLoadListener) {
        activity.getSupportLoaderManager().initLoader(type, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                CursorLoader cursorLoader = null;
                if (id == TYPE_IMAGE) {
                    cursorLoader = new CursorLoader(
                            activity, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            IMAGE_PROJECTION, MediaStore.Images.Media.MIME_TYPE + "=? or "
                            + MediaStore.Images.Media.MIME_TYPE + "=?",
                            new String[]{"image/jpeg", "image/png"}, IMAGE_PROJECTION[2] + " DESC");
                } else if (id == TYPE_VIDEO) {
                    cursorLoader = new CursorLoader(
                            activity, MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            VIDEO_PROJECTION, null, null, VIDEO_PROJECTION[2] + " DESC");
                }
                return cursorLoader;
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                ArrayList<LocalMediaFolder> imageFolders = new ArrayList<LocalMediaFolder>();
                //总图片文件夹
                LocalMediaFolder allImageFolder = new LocalMediaFolder();
                //总图片列表
                List<LocalMedia> allImages = new ArrayList<LocalMedia>();

                while (data != null && data.moveToNext()) {
                    // 获取图片的路径
                    String path = data.getString(data
                            .getColumnIndex(MediaStore.Images.Media.DATA));
                    File file = new File(path);
                    if (!file.exists())
                        continue;
                    // 获取该图片的目录路径名
                    File parentFile = file.getParentFile();
                    if (parentFile == null || !parentFile.exists())
                        continue;

                    String dirPath = parentFile.getAbsolutePath();
                    // 利用一个HashSet防止多次扫描同一个文件夹
                    if (mDirPaths.contains(dirPath)) {
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                    }

                    if (parentFile.list() == null)
                        continue;
                    File[] files;
                    //图片
                    if (type == TYPE_IMAGE) {
                        files = parentFile.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String filename) {
                                if (filename.endsWith(".jpg")
                                        || filename.endsWith(".png")
                                        || filename.endsWith(".jpeg"))
                                    return true;
                                return false;
                            }
                        });
                    }
                    //视频
                    else {
                        files = parentFile.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String filename) {
                                if (filename.endsWith(".mp4")
                                        || filename.endsWith(".rmvb")
                                        || filename.endsWith(".rm")
                                        || filename.endsWith(".avi")
                                        || filename.endsWith(".wmv")
                                        || filename.endsWith(".dmv")
                                        || filename.endsWith(".flv"))
                                    return true;
                                return false;
                            }
                        });
                    }
                    //当前图片文件夹
                    LocalMediaFolder localMediaFolder = getImageFolder(path, imageFolders);
                    //当前图片列表
                    ArrayList<LocalMedia> images = new ArrayList<>();
                    for (int i = 0; i < files.length; i++) {
                        File f = files[i];
                        LocalMedia localMedia = new LocalMedia(f.getAbsolutePath());
                        //设置图片最后编辑时间
                        localMedia.setLastUpdateAt(f.lastModified());
                        //保存图片到总图片列表
                        allImages.add(localMedia);
                        images.add(localMedia);
                    }
                    //保存当前文件夹图片数据
                    if (images.size() > 0) {
                        //排序当前文件夹图片
                        sortImage(images);
                        localMediaFolder.setImages(images);
                        localMediaFolder.setImageNum(localMediaFolder.getImages().size());
                        imageFolders.add(localMediaFolder);
                    }
                }
                //排序所有图片
                sortImage(allImages);
                allImageFolder.setImages(allImages);
                allImageFolder.setImageNum(allImageFolder.getImages().size());
                allImageFolder.setFirstImagePath(allImages.get(0).getPath());
                allImageFolder.setName(activity.getString(com.yongchun.library.R.string.all_image));
                imageFolders.add(allImageFolder);
                sortFolder(imageFolders);
                imageLoadListener.loadComplete(imageFolders);
                if (data != null) data.close();
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
            }
        });
    }

    /**
     * 图片排序
     * 按时间先后顺序，最新图片拍前面
     */
    private void sortImage(List<LocalMedia> mediaList) {
        Collections.sort(mediaList, new Comparator<LocalMedia>() {
            //second：需要比较的数据 first：被比较的数据
            public int compare(LocalMedia second, LocalMedia first) {
                //时间为0的数据排在末尾
                if (second.getLastUpdateAt() == 0) {
                    return 1;
                }
                //新图片排在前面
                if (second.getLastUpdateAt() > first.getLastUpdateAt()) {
                    return -1;
                }
                //旧图片排在后面
                else if (second.getLastUpdateAt() < first.getLastUpdateAt()) {
                    return 1;
                }
                //图片时间一致通过，文件名称排序
                else {
                    return second.getPath().compareTo(first.getPath());
                }
            }
        });
    }

    /**
     * 文件夹按图片数量排序
     *
     * @param imageFolders
     */
    private void sortFolder(List<LocalMediaFolder> imageFolders) {
        Collections.sort(imageFolders, new Comparator<LocalMediaFolder>() {
            @Override
            public int compare(LocalMediaFolder lhs, LocalMediaFolder rhs) {
                if (lhs.getImages() == null || rhs.getImages() == null) {
                    return 0;
                }
                int lsize = lhs.getImageNum();
                int rsize = rhs.getImageNum();
                return lsize == rsize ? 0 : (lsize < rsize ? 1 : -1);
            }
        });
    }

    private LocalMediaFolder getImageFolder(String path, List<LocalMediaFolder> imageFolders) {
        File imageFile = new File(path);
        File folderFile = imageFile.getParentFile();

        for (LocalMediaFolder folder : imageFolders) {
            if (folder.getName().equals(folderFile.getName())) {
                return folder;
            }
        }
        LocalMediaFolder newFolder = new LocalMediaFolder();
        newFolder.setName(folderFile.getName());
        newFolder.setPath(folderFile.getAbsolutePath());
        newFolder.setFirstImagePath(path);
        return newFolder;
    }

    public interface LocalMediaLoadListener {
        void loadComplete(List<LocalMediaFolder> folders);
    }

}
