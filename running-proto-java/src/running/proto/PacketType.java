/*
 * Copyright 2013-2018 featherrun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package running.proto;

public final class PacketType {
	public static final byte BOOLEAN = 1;   //type value
	public static final byte BYTE = 2;      //type value
	public static final byte SHORT = 3;     //type value
	public static final byte INT = 4;       //type value
	public static final byte LONG = 5;      //type value
	public static final byte VAR = 6;       //type value
	public static final byte STRING = 7;    //type length value
	public static final byte OBJECT = 8;    //type length(fix int) value [UNSAFE]
	public static final byte ARRAY = 11;    //type type length values
	public static final byte LIST = 12;     //type type length values
	public static final byte MAP = 13;      //type type type length values
}
