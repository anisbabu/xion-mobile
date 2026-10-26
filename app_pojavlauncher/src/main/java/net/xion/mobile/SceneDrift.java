package net.xion.mobile;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

/**
 * Slow "breathing" drift of the home backdrop: a scale + pan on one image view, run on the
 * RenderThread as a hardware layer, so it costs almost nothing even on budget phones.
 */
public final class SceneDrift {
    private final ObjectAnimator animator;

    private SceneDrift(View scene) {
        scene.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        animator = ObjectAnimator.ofPropertyValuesHolder(scene,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.04f, 1.10f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.04f, 1.10f),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -14f * density(scene), 14f * density(scene)));
        animator.setDuration(38_000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.start();
    }

    private static float density(View v) {
        return v.getResources().getDisplayMetrics().density;
    }

    /** @return null when the layout has no scene view (e.g. an older layout variant). */
    @Nullable
    public static SceneDrift attach(@Nullable View scene) {
        return scene == null ? null : new SceneDrift(scene);
    }

    public void pause() {
        animator.pause();
    }

    public void resume() {
        animator.resume();
    }
}
