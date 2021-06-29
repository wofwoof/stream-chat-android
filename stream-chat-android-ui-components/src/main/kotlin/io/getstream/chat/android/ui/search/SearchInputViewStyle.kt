package io.getstream.chat.android.ui.search

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import io.getstream.chat.android.ui.R
import io.getstream.chat.android.ui.TransformStyle
import io.getstream.chat.android.ui.common.extensions.internal.getColorCompat
import io.getstream.chat.android.ui.common.extensions.internal.getDrawableCompat
import io.getstream.chat.android.ui.common.extensions.internal.use

public data class SearchInputViewStyle(
    @ColorInt val backgroundColor: Int,
    @ColorInt val textColor: Int,
    @ColorInt val hintColor: Int,
    val searchIconDrawable: Drawable,
    val clearInputDrawable: Drawable,
    val hintText: String,
) {
    internal companion object {
        operator fun invoke(context: Context, attrs: AttributeSet?): SearchInputViewStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.SearchInputView,
                0,
                0
            ).use { a ->
                val searchIcon = a.getDrawable(R.styleable.SearchInputView_streamUiSearchInputViewSearchIcon)
                    ?: context.getDrawableCompat(R.drawable.stream_ui_ic_search)!!

                val clearIcon = a.getDrawable(R.styleable.SearchInputView_streamUiSearchInputViewClearInputIcon)
                    ?: context.getDrawableCompat(R.drawable.stream_ui_ic_clear)!!

                val backgroundColor = a.getColor(
                    R.styleable.SearchInputView_streamUiSearchInputViewBackgroundColor,
                    context.getColorCompat(R.color.stream_ui_literal_transparent)
                )

                val textColor = a.getColor(
                    R.styleable.SearchInputView_streamUiSearchInputViewTextColor,
                    context.getColorCompat(R.color.stream_ui_text_color_primary)
                )

                val hintColor = a.getColor(
                    R.styleable.SearchInputView_streamUiSearchInputViewTextColor,
                    context.getColorCompat(R.color.stream_ui_text_color_primary)
                )

                val hintText = a.getText(R.styleable.SearchInputView_streamUiSearchInputViewHintText)?.toString() ?: context.getString(R.string.stream_ui_search_input_hint)

                return SearchInputViewStyle(
                    searchIconDrawable = searchIcon,
                    clearInputDrawable = clearIcon,
                    backgroundColor = backgroundColor,
                    textColor = textColor,
                    hintColor = hintColor,
                    hintText = hintText,
                ).let(TransformStyle.searchInputViewStyle::transform)
            }
        }
    }
}
