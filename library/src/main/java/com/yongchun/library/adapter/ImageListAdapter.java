package com.yongchun.library.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.yongchun.library.R;
import com.yongchun.library.model.LocalMedia;
import com.yongchun.library.view.ImageSelectorActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by dee on 15/11/19.
 */
public class ImageListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_CAMERA = 1;
    public static final int TYPE_PICTURE = 2;

    private Context context;
    private boolean showCamera = true;
    private boolean enablePreview = true;
    private int maxSelectNum;
    private int selectMode = ImageSelectorActivity.MODE_MULTIPLE;
    private float limitTime;

    private List<LocalMedia> images = new ArrayList<LocalMedia>();
    private List<LocalMedia> selectImages = new ArrayList<LocalMedia>();

    private OnImageSelectChangedListener imageSelectChangedListener;

    private int mediaType; //多媒体类型

    public ImageListAdapter(Context context, int maxSelectNum, int mode, boolean showCamera, boolean enablePreview, int mediaType, float limitTime) {
        this.context = context;
        this.selectMode = mode;
        this.maxSelectNum = maxSelectNum;
        this.showCamera = showCamera;
        this.enablePreview = enablePreview;
        this.mediaType = mediaType;
        this.limitTime = limitTime;
    }

    public void bindImages(List<LocalMedia> images) {
        this.images = images;
        notifyDataSetChanged();
    }

    public void bindSelectImages(List<LocalMedia> images) {
        this.selectImages = images;
        notifyDataSetChanged();
        if (imageSelectChangedListener != null) {
            imageSelectChangedListener.onChange(selectImages);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (showCamera && position == 0) {
            return TYPE_CAMERA;
        } else {
            return TYPE_PICTURE;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_CAMERA) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_camera, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_picture, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (getItemViewType(position) == TYPE_CAMERA) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.headerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (imageSelectChangedListener != null) {
                        imageSelectChangedListener.onTakePhoto();
                    }
                }
            });
            headerHolder.tvCamera.setText(ImageSelectorActivity.TYPE_IMAGE == mediaType ? context.getString(R.string.take_picture) : context.getString(R.string.take_video));
        } else {
            final ViewHolder contentHolder = (ViewHolder) holder;
            final LocalMedia image = images.get(showCamera ? position - 1 : position);

            Glide.with(context)
                    .load(new File(image.getPath()))
                    .centerCrop()
                    .thumbnail(0.5f)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .dontAnimate()
                    .into(contentHolder.picture);

            if (selectMode == ImageSelectorActivity.MODE_SINGLE) {
                contentHolder.check.setVisibility(View.GONE);
            }

            selectImage(contentHolder, isSelected(image));

            if (enablePreview) {
                contentHolder.check.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeCheckboxState(contentHolder, image);
                    }
                });
            }

            contentHolder.contentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ((selectMode == ImageSelectorActivity.MODE_SINGLE || enablePreview) && imageSelectChangedListener != null) {
                        imageSelectChangedListener.onPictureClick(image, showCamera ? position - 1 : position);
                    } else {
                        changeCheckboxState(contentHolder, image);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return showCamera ? images.size() + 1 : images.size();
    }

    private void changeCheckboxState(ViewHolder contentHolder, LocalMedia image) {
        boolean isChecked = contentHolder.check.isSelected();
        if (!isChecked && mediaType == ImageSelectorActivity.TYPE_VIDEO) { //选中&&视频列表
            int time = getRingDuring(image.getPath());
            if (time > limitTime * 1000) { //时间大于限制时间
                Toast.makeText(context, String.format(context.getString(R.string.video_limit_time), removeDecimalZero(limitTime + "")), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (selectImages.size() >= maxSelectNum && !isChecked) { //超出选择条数
            int msgId = mediaType == ImageSelectorActivity.TYPE_VIDEO ? R.string.message_max_num_video : R.string.message_max_num;
            Toast.makeText(context, context.getString(msgId, maxSelectNum), Toast.LENGTH_LONG).show();
            return;
        }
        if (isChecked) {
            for (LocalMedia media : selectImages) {
                if (media.getPath().equals(image.getPath())) {
                    selectImages.remove(media);
                    break;
                }
            }
        } else {
            selectImages.add(image);
        }
        selectImage(contentHolder, !isChecked);
        if (imageSelectChangedListener != null) {
            imageSelectChangedListener.onChange(selectImages);
        }
    }

    public List<LocalMedia> getSelectedImages() {
        return selectImages;
    }

    public List<LocalMedia> getImages() {
        return images;
    }

    public boolean isSelected(LocalMedia image) {
        for (LocalMedia media : selectImages) {
            if (media.getPath().equals(image.getPath())) {
                return true;
            }
        }
        return false;
    }

    public void selectImage(ViewHolder holder, boolean isChecked) {
        holder.check.setSelected(isChecked);
        if (isChecked) {
            holder.picture.setColorFilter(context.getResources().getColor(R.color.image_overlay2), PorterDuff.Mode.SRC_ATOP);
        } else {
            holder.picture.setColorFilter(context.getResources().getColor(R.color.image_overlay), PorterDuff.Mode.SRC_ATOP);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        View headerView;
        TextView tvCamera;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            headerView = itemView;
            tvCamera = (TextView) itemView.findViewById(R.id.camera);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView picture;
        ImageView check;

        View contentView;

        public ViewHolder(View itemView) {
            super(itemView);
            contentView = itemView;
            picture = (ImageView) itemView.findViewById(R.id.picture);
            check = (ImageView) itemView.findViewById(R.id.check);
        }

    }

    public interface OnImageSelectChangedListener {
        void onChange(List<LocalMedia> selectImages);

        void onTakePhoto();

        void onPictureClick(LocalMedia media, int position);
    }

    public void setOnImageSelectChangedListener(OnImageSelectChangedListener imageSelectChangedListener) {
        this.imageSelectChangedListener = imageSelectChangedListener;
    }

    /**
     * 获取视频播放时长
     *
     * @param mUri
     * @return
     */
    public static int getRingDuring(String mUri) {
        int time = 0;
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(mUri);
            mediaPlayer.prepare();
            time = mediaPlayer.getDuration();
            mediaPlayer.release();
            mediaPlayer = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return time;
    }

    /**
     * 去掉结尾为0的小数位
     *
     * @param target
     * @return
     */
    public static String removeDecimalZero(String target) {
        if (TextUtils.isEmpty(target)) {
            return "0";
        }
        if (!target.endsWith(".00") && !target.endsWith(".0")) {
            //带小数点，且最末数字为0 如：1.40
            if (target.contains(".") && target.endsWith("0")) {
                //去掉末尾的0
                return target.substring(0, target.length() - 1);
            }
            return target;
        }
        int dotIndex = target.indexOf(".");
        return target.substring(0, dotIndex);
    }
}
