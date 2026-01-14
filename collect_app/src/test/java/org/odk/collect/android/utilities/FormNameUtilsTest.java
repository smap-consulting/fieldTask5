/*
 * Copyright 2019 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.utilities;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.odk.collect.android.utilities.FormNameUtils.normalizeFormName;
import static org.odk.collect.android.utilities.FormNameUtils.formatFilenameFromFormName;

public class FormNameUtilsTest {

    @Test
    public void normalizeFormNameTest() {
        assertThat(normalizeFormName(null, false), is(nullValue()));
        assertThat(normalizeFormName("Lorem", false), is("Lorem"));
        assertThat(normalizeFormName("Lorem ipsum", false), is("Lorem ipsum"));
        assertThat(normalizeFormName("Lorem\nipsum", false), is("Lorem ipsum"));
        assertThat(normalizeFormName("Lorem\n\nipsum", false), is("Lorem  ipsum"));
        assertThat(normalizeFormName("\nLorem\nipsum\n", false), is(" Lorem ipsum "));

        assertThat(normalizeFormName(null, true), is(nullValue()));
        assertThat(normalizeFormName("Lorem", true), is(nullValue()));
        assertThat(normalizeFormName("Lorem ipsum", true), is(nullValue()));
        assertThat(normalizeFormName("Lorem\nipsum", true), is("Lorem ipsum"));
        assertThat(normalizeFormName("Lorem\n\nipsum", true), is("Lorem  ipsum"));
        assertThat(normalizeFormName("\nLorem\nipsum\n", true), is(" Lorem ipsum "));
    }

    @Test
    public void formatFilenameFromFormNameTest() {
        // smap: filenames now include source prefix "test--" in unit tests
        assertThat(formatFilenameFromFormName(null), is(nullValue()));
        assertThat(formatFilenameFromFormName("simple"), is("test--simple"));
        assertThat(formatFilenameFromFormName("CamelCase"), is("test--CamelCase"));
        assertThat(formatFilenameFromFormName("01234566789"), is("test--01234566789"));

        assertThat(formatFilenameFromFormName(" trimWhitespace "), is("test--trimWhitespace"));
        assertThat(formatFilenameFromFormName("keep internal spaces"), is("test--keep internal spaces"));
        assertThat(formatFilenameFromFormName("other\n\twhitespace"), is("test--other whitespace"));
        assertThat(formatFilenameFromFormName("repeated         whitespace"), is("test--repeated whitespace"));

        assertThat(formatFilenameFromFormName("Turkish İ kept"), is("test--Turkish İ kept"));
        assertThat(formatFilenameFromFormName("registered symbol ® stripped"), is("test--registered symbol stripped"));
        assertThat(formatFilenameFromFormName("unicode fragment \ud800 stripped"), is("test--unicode fragment stripped"));
    }
}
