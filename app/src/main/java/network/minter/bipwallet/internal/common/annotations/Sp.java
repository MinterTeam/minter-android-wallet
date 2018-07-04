package network.minter.bipwallet.internal.common.annotations;

import android.support.annotation.Dimension;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * AtlasAndroid. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Documented
@Retention(CLASS)
@Target({METHOD, PARAMETER, FIELD, LOCAL_VARIABLE})
@Dimension(unit = Dimension.SP)
public @interface Sp {
}
