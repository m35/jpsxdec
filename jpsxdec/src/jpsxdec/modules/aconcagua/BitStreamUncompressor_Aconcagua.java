/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.modules.aconcagua;

import javax.annotation.Nonnull;
import jpsxdec.psxvideo.bitstreams.BitStreamDebugging;
import jpsxdec.psxvideo.bitstreams.IBitStreamWith1QuantizationScale;
import jpsxdec.psxvideo.mdec.MdecBlock;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecContext;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;

/**
 * The Aconcagua video bitstream decoder.
 * <pre>
 * nnnn##+++++++++++++++++++*;:,:;*znnnzzz#################zzzzzzzzz*;,`   ``...``   `.,:,,.```.,:i*+znzznnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn
 * nnnn#####++++++++++++++++*;:,:;*znnnnzzz###############zzzzzzzzn#*:.`   ``.,.`    `.,,,.`````.:;i+znnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnxnnnnnn
 * nnnn####+++++++++++++++++*;:,:;*znnnnzzz###############zzzzzzznn#i:.`   `.,,.`     `.,,.`` ``.,;i*#nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn
 * nnnn####+++++++++++++++++*;:,:;*znnnnzz###############zzzzz#####+i:...``.,,,.`     `.,,.`` `..,:;i+++nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn
 * nnnn####+++++++++++++++++*;:,:;*znnnnzz###########zzzzzzzz+**i*++*****#zzzz+i,`   ``.:,.````..;::ii;:+nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn
 * nnnn####+++++++++++++++++*;:,:;*znnnnzz##########z#zzzzz#**;;i*+zxMM@W@@@@WWWni.` ``,:,,.```.;;i;ii;;;+nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn
 * nnnn####+++++++++++++++++*;:,:;*znnnnzzz#########zzzzzz#+*;:;i#nMMW@@W@@@@@@@@Wx+.``,::,.```.*iii***;;i*nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn
 * nnnn###++++++++++++++++++*;:,:;*znnnnzz#####z####z+zzzz+*;::innMMWMWxM@####@@@@@Wx+::::,.``,.*+*ii;;*i;i+nnznnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn
 * nnnn#++++++++++++++++++++*;:,:;*znnnnzz###;;*###zi;;*z#*i:;*xxMWMWWWWW#@@@@@@@@@@WWMzi:,.`;;;:+#*ii;;ii;iznnzznnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn
 * nnnn#++++++++++++++++++++*;:::;*znnnnzz#z;;;i#z+:;;;iz+i;;+MxxMMW@W@W@@@@@@@@@@@@@WMMx+:.`+;;;i*++*ii;;i:inznzzzzznnnnzzzzznnnnnnnnnnnnnnnnnnnnnnznnnn
 * nnnn#++++++++++++++++++++*i::;;*znnnnzzz*i;i*+;i*;;i*z+i#znMMxWW@@##@@####@@#@#@@@@WWWWn:`**i;;i*+z+*i;;i:*nzzzzzzzzzzzzzzznnnnnnnnnnnnnzzznnnnnnznnnn
 * nnnn#++++++++++++++++++++*i;;;;*#nnnnzz#**;*#+*ii;i*#nnxxxxWW@@@#@#@@####@@@@@@@@@@@@WW@M;.##ii;ii*++*i::i;inzzzzzzzzzzzzzzznznnnnnnnnnzzzznnnnnnznnnn
 * nnnn#+++++++++++++++++++++i;::;i+znnnzz**i*#+*i;;**#Wz+M@@@W@@@@#@@@@@@@@@@@@@@@@@@W@@WMWM*.i#+*;;ii*++i::iizzzzzzzzzzzzzzzzzznnnnnnnzzzzznnnnnnnnnnnn
 * nnnn#+++++++++++++++++++++i;,,:;*znnnn#+ii+**iiii*zM+;:+@@@@@@@##@####@@@@@@@##@@@@@@WMMMWM*..i#*i;;ii+#*;i*+zzzzzzzzzzzzzzzzzzzzznnzzzzzzzznnnnnnnnnn
 * nnnn##++++++++++++++++++++i:,.,:i#nnnz+**++ii;i*+zzi;;;*@@@##########@@#####@@@@@@@@@WxxMMxxz,`:#*;;:;i++i;*inzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzznnnnznnnn
 * nnnn##+++++++++++++++++++*;,...,i#nnn#****i;:;i+z+iii;i#@@########@##########@@@@@@@@@WMWMMxxz,.:#+i;:;i++ii*zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzznnnn
 * nnnn#++++++++++++++++++++*;,``.,;#nn#*iiii;:;*+#*i;;;i+M@###################@#@#@@@@#@@@W@WMMW*..:++*;;i*+*i*+zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzznnnn
 * nnnn###++++++++++++++++++*;,```,;+nz**i*i;:;**#*i;;;i#W#####################@@@@##@@##@@@W@WW@x:,,;+#*;i*++***nzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzznnnn
 * nnnn###++++++++++++++++++*;,```,i#n++*+ii:;+#+i;;;i*z@##################@@@@@@@@@###@@@@@@@@@@@+.,:i+#i;i*+***zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzznnnn
 * nnnn###++++++++++++++++++*;,``.,i#z+++*i;;+z*i;:;i+M@##########################@##@@@@@@@@#@@@@M:,:;**+i;*****zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzznnnn
 * nnnn###++++++++++++++++++*;,.`.:i#+++**;;*#*i;::i*M#######################@@###@##@@@#@@@@MznW@@*,:;**+*;i****+zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzznnnn
 * nnnn####+++++++++++++++++*;,...:*z+***i:*+*i;::i*n@######@#############@@@@##@@###@#@@@@#@z::;i*+:,;*++*;i*****nzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzznnnn
 * nnnn####+++++++++++++++++*;,..,;+#+**i;i#*i;::i*n@########@@########@@@###@@@@#@@@@@@@@@##n::;i;iii;*z+*i;i***izzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzznnnn
 * nnnn####+++++++++++++++++*;,,,,i#+++*;;#++i;;i+n@#@#####@@@@@@######@@@#@##@@#@@@#@@##@@##W+i;;;;i;i*+z*i;ii*+**zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzznnnn
 * nnnn##+++++++++++++++++++*;,,,:i+***i;*#*ii;i*z@@@###Wxn@@WWWW###@@@@@@@@@@@@@@@@@#@#######Wz*iii;;;ii*+*;;i*i*izzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzznnnn
 * nnnn###+++++++++++++++++++;,,,,i*+**ii#*i;;i*iM@##Wx#;::x@WWMWW@@@@@@####@@#@@@@#@@@########@Wz*ii;;;i*+*i;;*i;i*zz#zzzzzzzzzzzzzzzzzzzzzzzzzzzzzznnnn
 * nnnn##+++++++++++++++++++*;,,,,;****;++i;;;*i+@#@xiii:::nWWWMMMW@@@##@@@@@@@@@@@@@@###########@Wzii;ii****i;i**ii#zz#z#z########zzzzzzzzzzzzzzzzzznnnn
 * nnnn#+++++++++++++++++++++;,,,,i+*iii+ii;;**ix#W#i;;;;;iMWMMMMxMWW@@@@@@@@@@@@@@@@@@###########@@W#*iiii***ii***iiz#zz#############zzzzzzzzzzzzzzznnnn
 * nnnn#+++++++++++++++++++++;,,.:+*iii+*i;;*+*#Wni;i;;;;*x@WWWMxznxxWWW@@@@@@@@#@@@@@@@@##########@@M++*i;ii***ii*i;*z#z##########zzzzz##zzzzzzzzzzznnnn
 * nnnn#++++++++++++++++++++*;,.,*iiiii*ii;i+*#z*i;:iiii#W@WWWWxn#zzznnxMM@@@@@@@@@@@##@@@###########@;*+*iiiii******izz###################zzzzzzzzzznnnn
 * nnnn#++++++++++++++++++++*;,.iii*i**i;;;i**+*ii:;;i+M@@@@@WWWz+#nn###xxMW@@@@@@@@@@#@@#@########@@#ziz#*i;i*i****ii*z#####################zzzzzzzznnnn
 * nnnn#+++#++++++++++++++++*;,:******iii;ii+**iii;;;#@@@@@@WWMMnz##zzz++zxMWWW@@@@@@@@@WMWW@@@###@@@@M*z+i;;;ii*ii*ii;######################zzzzzzzznnnn
 * nnnn#++++++++++++++++++++*;,;***i**ii;;;**iiii;;;z@@#@@@@@WMnnz#+####++##xWWWWW@@W@@@WxnnxxW######@W*##*;;;ii*iii+*ii#####################zzzzzzzznnnn
 * nnnn#++++++++++++++++++++*i:******iiiii**ii;;;i*x@@@#@@@@WMnnnz+*+#++*iii++xxnM@WMnMMzzzzznn@@@@@##@+*++i;;iiiiii*+i:+###################zzzzzzzzznnnn
 * nnnn#++++++++++++++++++++*ii**+i*iii***ii;;;;iin###@@@@@@Mxxzznz#+++iiii;+iinz#nMnz#z#znnnnnM@@@@##@##Mn+;;;iiiiii**:;###################zzzzzzzzznnnn
 * nnnn#+++++++++++++++++++++i+****i***ii;;;;;ii*x@###@#@@WMnnnzznnz#*#+**+i+i:++*##zznznxxxnnnx@@@@##@#xWxn+i;iiiiiiiii;*#############zzz#zzzzzzzzzznnnn
 * nnnn#+++++++++++++++++++++*********ii;;;;;i*zW@######@Wxzzznzzznnzzxnznz*+i;*#i+zzxMMnnxnn#nnW#@@####M@Wzi;;;iiiiii*i*;###############zzzzzzzzzzzznnnn
 * nnnn#++++++++++++++++++++++*+**i+*iii;;;;;;*M######@@WMzzznnzz#z#zxnnnz##+*ii#++znnnnnnnzz#znM@#####zn@@n*;;;;ii;;ii*ii+#############zzzzzzzzzzzzznnnn
 * nnnn#++++++++++++++++++++***+*i**iiii;;;;ii#@#####@@@Wnzzznz####znn##++zz##+**#z#+iiii*#zz#znx@#####z#nWM*i;;;;iii;i***i+z###z#####zzzzzzzzzzzzzzznnnn
 * nnnn#++++++++++++++++++++**+*i**iiiiii;;;i*W#######@Wxzzzzn#####+ii*+**++#+ii*#+i*#zz###+z#znx@####@z#zzx#i;;:;ii;;ii*++i#z#zzz###zzzzzzzzzzzzzzzznnnn
 * nnnn#+++++++++++++++++++*******ii;;;;;;;;ix@######@@Mnzzzzz###+*+znnxn##+#+ii+#++znxW@WMn###nnW####W#z#zz+ii;;;i;;;iiii;iizz###zzzzzzzzzzzzzzzzzznnnnn
 * nnnn#++++++++++++++++++*******i;;;;ii;;i+M########@@Mnzzzzz+++#xMW@Wzxz+++#***##*z#+#z#zx###nzW####x#zzzz#iii;;;;i;;;;ii,:*z#z#zzzzzzzzzzzzzzzzzznnnnn
 * nnnn##+++++++++++++++++**+***ii;;;;;;;;;*@########@@xzz##z#+++nz+++**++**##+**#z*+++**i+#z##zzM###@##zzzzz+*iii;;;;;i;;i;,:+zzzzzzzzzzzzzzzzzzzzzznnnn
 * nnnn####++++++++++++++*i*+**iii;;;;;;;;i+W########@Wxz#####+##+*iii*+##++#+***#z#*+*++*+++#+zzM###W+#zzzzz#iii;;;;;;;;;;i:::+nzzzzzzzzzzzzzzzznnnnnnnn
 * nnnn####++++++++++++++*****i;;;;;;;;;;i*#M#########Wxzzz###+++**********+#+***###*iiii****++#zM##@x+#zzzzzz*iii;ii;;;:;;;i;;;+nzzzzzzznzzzzznnnnnnnnnn
 * nnnn####+++++++++++++*i***ii;;;;;;;;;;i+#z@#######@Mnzz###+++***iiiiiii*+++***+##+*iiii**+++##x##@#+##zzzzz#****i;;;:::;;;*i;;#nzzzzznnnnnnnnnnnnnnnnn
 * nnnn####++++++++++++*i**iiii;;;;;;;;;i*###@#@@####@xnz####+++***iiiiii*+++++**###+***iiii*++##x#@@*+##zzzzzz#**iii;;::::;;iii;;znnnzznnnnnnnnnnnnnnnnn
 * nnnn##++++++++++++++***iii;;;;;;;;;;ii*##+x#@@@##@@xnz###++***iii;ii*****+++**+##++***iiii*+##x##@i+zzzzzzzzz#*++*i;:::;;;;ii;:;nnznnnnnnnnnnnnnnnnnnn
 * nnnn#+++++++++++++*i*i*i;i;;;;;;;;;;i*###+z@@@@@@@@xnz####**iii;;i***iii+++*ii*+#+****iii***##x##x;+zzzzzzzzzzz#++i;:::;:;;i*i::innnnnnnnnnnnnnnnnnnnn
 * nnnn##+++++++++++i****iii;;;;;;;;;;i**###++W#@@@@@@xzz####+*i;;;iiiiiii*++*ii;i*##*i*iiii*++##x#W+i##zzzzzzzzzzz##*;;;:::;;ii*i;;innnnnnnnnnnnnnnnnnnn
 * nnnn##++++++++++i****i;;;;;;;;;;;;i**+z##++n@WMW@@WMzzz#+#+*ii;;;;iiiii+#+*i;:;*+##*ii;ii*++##xW#;i+zzzzzzzzzzzzz#+ii;;:::;;;iii;:*nznnnnnnnnnnnnnnnnn
 * nnnn##+++++++++******i;;;;;;;;;;i***+zz##++z@WMW@@Wxzz####+**i;;;;;ii;*#++*;;;;i+##+iiiii*+#####i:i#zzzzzzzzzzzzzz#+*;;;;:;;;i;i;;:+nnnnnnnnnnnnnnnnnn
 * nnnn##+++++++#+*****i;;;;;;;;;;ii**+zzz##+++WMWWWWMnz#####++**i;;;;;;*#+***iiii*++##*iiii*+###z+::i#zzzzzzzzzzzzzn##+i;;::;;;;i;;;::znnnxnnnnnnnnnnnnn
 * nnnn###+++++#+*i****i;;;::;;;iii**+znzz##+++xMMWWWzzz#####++***ii;;;i+#+****iii*#++#+ii***+####+,:i#zzzzzzzzzzzzzzz#+*i;;;;;;;;i;;;::#+;;*nnxnnnnnnnnn
 * nnnn###+##+#+ii****i;;;;;;;;;***++znzz###++*#MxW@W#zzz#####++**iiiii**+##xM#+*+zMn#++*i***+##zii,:i#zzzzzzzzzzzzzznz++i;;;;;;;;;i;;::;+i;:i:*#nnnnnnnn
 * nnnn####i##+******ii;;;;;;;ii+##+zzzzzz##++*ixxMMWn#zz#z#++++***iiii*ii+##zzzz##zz#+*****+#+##*:.:i#zzzzzzzzzzzzzzzn#++i;;;;;;;;ii;;;:+#i:;:ii*#nnnnnn
 * nnnn##+:,i+*i****ii;;;;;;;;i*#i:*#zzzzz##++*;zMxxnzz#zzz##++++**iii*iii*****++**+++++****+#+##*;.,i+zzzzzzzzzzzzzzznz++*ii;;;;;;;ii;;;+nx*:;;:;+nznnnn
 * nnnn#+*ii:+i*****i;;;;;;;;i*#+,:i#nzzzz##++i;*Mxn+#z#zzz#+++++***i*iiii*****ii**+++*++**++###++:`,;+zzzzzzzzzzzzzzzzz#+**i;;;;;;;;ii;;+zxzii;:;znznnnn
 * nnnn#+;i+n#****ii;;;;;;;;i*+#,,:i#zzzzz##++i;;Mxz+##zzz##++*++*****iiii*****iii*+++*++++++#+#+*``,;+#zzzzzzzzzzzzzzznz+**ii;;;;;;;iiii+#nx+ii;innnnnnn
 * nnnn##*;i#+****i;;;;;;;;ii*#;,,:*#nnzzz##+*i;:x@z+##z#####+**+******iii*i*******++++++++++#+#i` `.:*#zzzzzzzzzzzzzzzzz#++*iii;;;;;iii*+zz#i:;ii#nnnnnn
 * nnnn##**;++++*ii;;;;;;;;i*++::,:*#zzzzz##+*i;,##@xnMx###z#+**********ii***++**+#+++++#+++++##,` `.:i#zzzzzzzzzzzzzzzzzz+***iii;;;;;;i;*;;,;ii*;;nznnnn
 * nnnn#i;i+;*#+*ii;;;;;;ii*++i,,,:i#zzzzz##+*i:,*@##@@Mzz#z#+*****i***+ii**+########+*+#+++++#+,`  `,;+zzzzzzzzzzzzzzzzzn#**ii;i;,:;i:;,;;,:::;ii:+nnnnn
 * nnnni;ii*i*#+*i;;;;;;ii*+++;,..,;+zzzzz##+*i::;W@##@xz#z##++*********ii*#zznnzxnzz#++++++++#*:.  `.:+zzzzzzzzzzzzzzzzz+;i+**i;*ii:iii*;:,::;;ii;;znnnn
 * nnnnii;iii+#+**i;;;;ii**++*;,..,:*#zzzz##+*i:,,z##@Mxzzzz#++***********#nxnnxxxnnn##+++++++#i:,. `.:*#zzzzzzzzzzzzzzzz*ii#*i***+ii;**;;;;;;;;;;i:#nnnn
 * nnnn*i**iiz#++*i;;;ii**+++*;,``.:i#zzzz##+*i;,,;W#@zzzzz##++**********+*+z#+###+#z+#+++++++#n;,,` `,;+zzzzzzzzzzzzzzzzzz+*********i*ii:;;;;iii;;;#nnnn
 * nnnn***;:*##+***i;iii*++++*:.```,i#nzzz##+*i;:,,+#+i+zzz####*******ii**++##+***+##+++++++++xx+,..``.:*#zzzzzzzzzzzzzzzzz+i*ii*******;;;;;::;;iiii#nnnn
 * nnnn**i:i+#+++**iii**+++++*:.` `,;#nzzz##+*i;:,,,,,:*#zz####++******ii**++###+###+*+++++++#@M#i,:.``,;*#zzzzzzz#########+iiii******ii;;i;:;;;;;ii#nnnn
 * nnnn+i;i*;+++******+++++*+*:.```,;#nzzz##+*i;:,,,,,;i+zzz####+****+*******+#zzz#++*+++++++M@Mz#;:,` `,;*+###+++****iiiiiii*iii******iii;;;;;iiii*#nnnn
 * nnnn*;i*ii*******+++++#*i+*;.```,i#nzzz##+*i;::,::;*i+########+*+++********+###+**++++#++#@@Mz#*:,,` `,,;ii;::;;;:::,,::;*iiiii******iiii;;;;;;;iznnnn
 * nnnniiiiii;;****+++++#i*i+*;,...:i#nnzz##+*i;;:;i++*i;########++++#+***+**+*+++*++++*++++n@@xz#+i,.````,;;;;;::;;:,...,:;iii*ii*******iii;ii;i;;;znnnn
 * nnnn*ii;;ii*+*iiii+***+*++*;,...:*znzzzz##**;;i*++*i*;+########+*+#+***++***i******+*+#+#M@@xz#++;``.`:i;i*;;;iii;:::;i*+iii*ii****+***ii;;;;i;i;#nnnn
 * nnnn*i;;;;i*+#+i**++##+#++*;,..,;+znnn###+*i:i**#+iii*i+#+#####+++#+****i*iii;ii**i**+#+#M@Wx###+i,..;*i***ii;i;i;:;**++z*iiiiiii*++***iiiiii;;ii#nnnn
 * nnnn*i;;iii****++###z++#++*;,,,:i#nnzi;;:;;;*z+++*;i**i;+####z##++##+ii*iiiiii;ii*i**###zM@Mn##++;::.i+*+++ii;;ii;i+++#nn*i;;iiii*******i;;;i;;;;#nnnn
 * nnnn*;;;iii*i*++*++++*i*++*;:::::;#z**+ii;;i+n#*+;;i**iii*#######+##+*iii;;;;i;;ii*++###zWWxz###*i;::;*++#+**;i+*i*#++#n*ii;;;iii***i****ii;;;;;;#nnnn
 * nnnniiiiiii***+*+*+*+*i::;:,:::;:::i+z#*+**i+z#*+;i****ii;i+#########+*iiiiiiiii***++###zWMnz###*;;i;i+#+++i**i#+i*z+#n**z*i;;;;iii**i+*ii;;;i;;;#nnnn
 * nnnnii;i**ii*++***+***+i::;;;;iii*i:izz++#+*i+#+*i*******i;i*zz####+#++************+####zMMn####*;;i**+++++***;z+i+z+n+*#z*i;;;;;ii**i**iii;;;;;;#nnnn
 * nnnni;;ii***+****++***#*;:;;ii******;*n#*+z+i;#+*i*ii******i*#zz######+++********++####zzxMz#+++iiiii#++zz+***i##i#zzz*#+++ii;;;;;iii*i*iiiiii;;;#nnnn
 * nnnn*i;iii**++***++***#*i;:;i*+**+++ii+z++z#i;i++**i******ii+i#znzz##+#+#++**+++++####zz#nnn#+++ii;i*ii+#z*****#z*z#n++++z#iiii;iiiiii**i*ii;;;;;#nznn
 * nnnnii;ii*+******+****n*ii;:;i****++**i+#+z#*i;i++*iii***+***i*#nnzzz##++++++*++++#####z+z#z#++i;ii+*i**+zii***#z*zz++++#+**iii;;iii*i**i*ii**iii#nnnn
 * nnnniiii***i****++****x+ii*i;:i***+++*i+++z#+i;;i+*ii;ii*++**iiznnz########++##+######z*#####++ii**+***++#i;*i*+n*zz*+++**+i*i;;;;iiii****iiiiii;#nnnn
 * nnnni;;i*i******+***+*x#*i*+*i;***+++*i**+#z#+i;i***iii;****+*;*+xn#########zz####+##zi;z#####+i;i*+**+*++;:i*i*z+z#*++*+***i*;;i;;iii*****;ii;;i#nnnn
 * nnnn*ii**i***+*+***i**x#*i****i***+++*i****z#+i;;i*i;ii;;i****;i+zxzzz######++######+;,*z++#++*ii+nz**+*+*;;i***+#z+****+*++iii;;i;i*******i*ii;;#nnnn
 * nnnn*ii*ii*i+*++******x#*i*********++****i*#z#+i;ii;;iii;;iii*i+++nz##++++**++++++*;..:+#+++*iii*++*,**i+i;;:****##***++***+*i**;iii;***++*ii;;;i#znnn
 * nnnn*****i**+*+******+W#*i*********#+**i**++##+*;;i;;;;ii;;ii;i#z###+#####++*i;:.```..i++**;;ii*iii:.:*i+*;;;ii**#z+***+***##i*ii;iiii**+****i;;;#nnnn
 * nnnn+****+*+*++*****i+@z*i*********#+*ii****+#+i;;ii;;;iii;iii;+*;........````  ```..,iiii;i;iiiiii:.:***i;;i**i*+n+**++**+#+**iiiiiiiii*+***iiii#nnnn
 * nnnn***++****++*****i+Wz*i**i+*i***##**i*****+*ii;;i;;;;;iii;i;+*:.`             ``..,,;i;i;;i;iiii:.i***iii;i*i*+n#**+***++******ii;iiiii*+**iii#nnnn
 * nnnn********++***+***+xz**+*******+##*i*i**i***ii;;ii;;;i;iii;i*+:.             ``...`,iii;;;ii;ii*:.i*iii;i;i+ii*zz*********i****i**i;ii*******i#nnnn
 * nnnn*ii**ii**********+nz+*********+##***i**ii;i*i;:;i;;;iii;ii*zz#i.`    `````````````:ii;;;;;;;iii:.i**iiii;;*i;*#n****i++*iii*******iii********zznnn
 * nnnn*;;;:ii*;;i;**i;;;i#+******i**+z#**i;i*i;;iii;;;ii;;;;i;;;*++#*:.``   `     ``````ii;;;;i;;;;ii:,***iiii;i*i;i+n+******iiiiii*i*************iznnnn
 * nnnn+;;;,i;;i;:;;i;;:;*z*i*****i**+z+*i;;i*i;;iiii;;;i;::;iiiii;;;:.``          `````.*;;;;;:;;iii;,,i***ii;;;**;;inz******;;;iiiiiii*********+*i#nnnn
 * nnnn*ii**+**ii**i;i+ii+z+***i*****+z+*i;;;*i;;;iii;:;i;:;i;;iiiiii,..```       `` ``..*;i;;;:;;;ii;,,***ii;::;**i;;#n*+***i;;;iii;ii;iii*****++**#nnnn
 * nnnnii*++++****iii****+z+***i*****+z+;ii;i;;i;;ii*;;;ii;;;;i;;i;ii,.,.``      `` ` .`.;i;::;;:;;ii;:;****ii::;**i;:ix+****i;i;iiii;;i;;ii*i*i**++zznnn
 *</pre>
 *
 * Aconcagua has 2 videos: an into video and an ending video. Each one has totally
 * different lookup tables found in {@link AconcaguaIntroVideoTables} and
 * {@link AconcaguaEndingVideoTables}, but how they're processed is the same.
 *
 * For each block
 *  - Read the DC code (normal)
 *  - The bits either point to a predefined VLC lookup table (normal)
 *  - Or there is an escape code indicating the DC value is in the
 *    bitstream itself (normal)
 *  - The chroma DC values are relative to prior chroma values (normal)
 *  - The luma DC values are relative to prior luma DC values (normal)
 *  - The choice of crisscrossing which luma block DC codes are relative
 *    to others is... (odd)
 *  - Now you must read an instruction code that tells us how we are going to
 *    decode this block (wait... what?)
 *  - There are only 64 predefined list of instructions, and there is no
 *    escape code to offer an alternative number (that would mean.. how?)
 *  - The instruction will tell us how many AC VLC codes to read from 3
 *    more different tables (and 3 more??)
 *  - So you follow the instructions, and read X VLC codes using the first
 *    table, Y codes using the second table, and Z codes using
 *    the third table (uh, sure)
 *  - Each table comes with it's own escape code to allow for entries not
 *    in the lookup table (back to some normalcy)
 *  - After following the instructions, the block is over (ok)
 *  - The bit reader used for reading the bit stream is actually pretty cool.
 *    The bits are read backwards from how normal bitstreams work, which makes
 *    the reader extremely simple and likely pretty fast. (cool)
 *  - And after reading one vertical column of macroblocks, you reset all the
 *    DC values to 0 (alright, that's fairly reasonable)
 *    and you skip up to 4 bytes in the original bitstream (4 bytes just
 *    wasted??)
 *
 * What even.......
 *
 * There's no escape code to do some unique number of table lookups among the
 * three tables. You're stuck with them. Which means there will be cases where
 * you really don't need to read N values from a table--too bad, because you're
 * reading that preset count no matter what. Or maybe it would be better to read
 * more from one of the tables to save some bits--nope! Or maybe you need to
 * read only N number of codes in a block, but there is no X+Y+Z=N. What do you
 * even do???
 *
 * Now consider this from the perspective of the insane encoder someone had to
 * write. How do you choose how many values need to be read from each table. And
 * even more insane, how were the final instruction table values chosen in the
 * first place????? And why are there 3 tables to read from???? Why not 2? Or
 * 4? And how were the values of those tables even chosen as well????????
 *
 * Now let's say maybe all this somehow actually saved some space in the frame
 * to allow for a better quality image. Those few *BIT* savings are completely
 * blown away by wasting at least a dozen *BYTES* per frame.
 *
 * And all this insanity is compounded by the fact that there is the standard
 * PlayStation bitstream formats that
 * - are readily available
 * - encode better
 * - are better quality
 * - decode faster
 * - and the decoding logic is already written for you
 * - and so is the encoder!!
 *
 * Having said all that, we all make poor design decisions in our lives. Trying
 * to spin up your own version of some other program because you could do it
 * better--only to be wrong, but you're stuck with it now. It happens to all of
 * us. I'm sure someone in the development team wished they could go back and do
 * it differently.
 */
public class BitStreamUncompressor_Aconcagua implements IBitStreamWith1QuantizationScale {

    public @Nonnull BitStreamUncompressor_Aconcagua makeFor(@Nonnull byte[] abBitStream) {
        return new BitStreamUncompressor_Aconcagua(_tables, MACROBLOCK_HEIGHT, _iQuantizationScale, abBitStream);
    }

    /** Both videos have this height. */
    // TODO should probably dynamically calculate this on the height of the frame as it is read
    static final int MACROBLOCK_HEIGHT = 13; // 208 / 16

    public static final int MINIMUM_BITSTREAM_SIZE = AconcaguaBitReader.MINIMUM_BITSTREAM_SIZE;

    private final int _iQuantizationScale;
    @Nonnull
    private final MdecContext _context;
    @Nonnull
    private final AconcaguaHuffmanTables _tables;

    private int _iBlocksLeftInColumn = MACROBLOCK_HEIGHT * 6;

    public BitStreamUncompressor_Aconcagua(@Nonnull AconcaguaHuffmanTables tables, int iMacroblockHeight,
                                           int iQuantizationScale, @Nonnull byte[] abBitstream)
    {
        _tables = tables;
        _iQuantizationScale = iQuantizationScale;
        _bitstream = new AconcaguaBitReader(abBitstream, 0);
        _context = new MdecContext(iMacroblockHeight);
    }

    @Override
    public @Nonnull BitStreamCompressor_Aconcagua makeCompressor() throws UnsupportedOperationException {
        return new BitStreamCompressor_Aconcagua(_tables, _iQuantizationScale);
    }

    @Override
    public int getQuantizationScale() {
        return _iQuantizationScale;
    }

    @Override
    public int getBitPosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getByteOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean skipPaddingBits() {
        return true;
    }

    // -------------------------------------------------------------------------
    @Nonnull
    private final AconcaguaBitReader _bitstream;
    private int _iPreviousDcCr = 0;
    private int _iPreviousDcCb = 0;
    private int _iPreviousDcSetInY1UsedInY2Y3 = 0;
    private int _iPreviousDcSetInY3UsedInY1Y4 = 0;

    private int _iTable1Reads = -1;
    private int _iTable2Reads = -1;
    private int _iTable3Reads = -1;

    private void resetColumn() throws MdecException.EndOfStream {
        _iPreviousDcCr = 0;
        _iPreviousDcCb = 0;
        _iPreviousDcSetInY1UsedInY2Y3 = 0;
        _iPreviousDcSetInY3UsedInY1Y4 = 0;
        _bitstream.resetColumn();
        _iBlocksLeftInColumn = MACROBLOCK_HEIGHT * 6;
    }

    // -------------------------------------------------------------------------

    private static final int BOTTOM_10_BITS = 0x03ff;

    @Override
    public boolean readMdecCode(@Nonnull MdecCode code) throws MdecException.EndOfStream, MdecException.ReadCorruption {
        if (_context.atStartOfBlock()) {
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(_context.toString());
            if (_iBlocksLeftInColumn == 0)
                resetColumn();
            _iBlocksLeftInColumn--;

            int iNext32Bits = _bitstream.getBits();
            int iBitsToSkip = readDc(_context.getCurrentBlock(), code, iNext32Bits);
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("%d bits for qscale/dc %s", iBitsToSkip, code));
            _bitstream.skipBits(iBitsToSkip);
            _context.nextCode();
            return false;
        } else {
            if (_context.getMdecCodesReadInCurrentBlock() == 1) {

                int iNext32Bits = _bitstream.getBits();
                InstructionTable.InstructionCode instruction = _tables.lookupInstruction(iNext32Bits);
                assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("%d bits for instruction %s", instruction.getBitCodeLen(), instruction));
                _bitstream.skipBits(instruction.getBitCodeLen());

                _iTable1Reads = instruction.getTable1Count();
                _iTable2Reads = instruction.getTable2Count();
                _iTable3Reads = instruction.getTable3Count();
            }

            if (_iTable1Reads > 0) {
                int iNext32Bits = _bitstream.getBits();
                int iBitsToSkip = _tables.readAcTable1(code, iNext32Bits);
                assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("%d bits for rle/ac %s", iBitsToSkip, code));
                _bitstream.skipBits(iBitsToSkip);
                _iTable1Reads--;
                _context.nextCode();
                return false;
            }

            if (_iTable2Reads > 0) {
                int iNext32Bits = _bitstream.getBits();
                int iBitsToSkip = _tables.readAcTable2(code, iNext32Bits);
                assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("%d bits for rle/ac %s", iBitsToSkip, code));
                _bitstream.skipBits(iBitsToSkip);
                _iTable2Reads--;
                _context.nextCode();
                return false;
            }

            if (_iTable3Reads > 0) {
                int iNext32Bits = _bitstream.getBits();
                int iBitsToSkip = _tables.readAcTable3(code, iNext32Bits);
                assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("%d bits rle/ac %s", iBitsToSkip, code));
                _bitstream.skipBits(iBitsToSkip);
                _iTable3Reads--;
                _context.nextCode();
                return false;
            }

            code.setToEndOfData();
            _context.nextCodeEndBlock();
            return true;
        }
    }

    private final DcTable.DcRead _dcRead = new DcTable.DcRead();
    private int readDc(@Nonnull MdecBlock block, @Nonnull MdecCode code, int iNext32Bits) throws MdecException.ReadCorruption {

        switch (block) {
            case Cr:
                _tables.readDc(_dcRead, iNext32Bits, _iPreviousDcCr);
                _iPreviousDcCr = _dcRead.iDc;
                break;
            case Cb:
                _tables.readDc(_dcRead, iNext32Bits, _iPreviousDcCb);
                _iPreviousDcCb = _dcRead.iDc;
                break;
            case Y1:
                _tables.readDc(_dcRead, iNext32Bits, _iPreviousDcSetInY3UsedInY1Y4);
                _iPreviousDcSetInY1UsedInY2Y3 = _dcRead.iDc;
                break;
            case Y2:
                _tables.readDc(_dcRead, iNext32Bits, _iPreviousDcSetInY1UsedInY2Y3);
                break;
            case Y3:
                _tables.readDc(_dcRead, iNext32Bits, _iPreviousDcSetInY1UsedInY2Y3);
                _iPreviousDcSetInY3UsedInY1Y4 = _dcRead.iDc;
                break;
            case Y4: default:
                _tables.readDc(_dcRead, iNext32Bits, _iPreviousDcSetInY3UsedInY1Y4);
                break;
        }

        code.set((_iQuantizationScale << 10) | (_dcRead.iDc & BOTTOM_10_BITS));

        return _dcRead.iBitLen;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + _context + " current 4 byte start=" + _bitstream._iPositionMultOf4;
    }


    /**
     * This bit reader is actually pretty clever. It works backwards from the
     * normal bitstream approach, which I think might make it faster. But then
     * again, the mpeg1 spec chose its bitstream style for a reason.
     */
    private static class AconcaguaBitReader {

        /** 12 bytes are buffered immediately, so make sure the bitstream
         * buffer is at least that big for the initialization. */
        public static final int MINIMUM_BITSTREAM_SIZE = 4 + 4 + 4;

        private int _iFrontBits;
        private int _iMidBits;
        private int _iBackBits;
        private int _iBitsRemaining;
        @Nonnull
        private final byte[] _abBitstream;
        private int _iPositionMultOf4 = -4; // meh hack so I can call resetColumn() in the constructor

        public AconcaguaBitReader(@Nonnull byte[] abBitstream, int iStartPos) {
            if (abBitstream.length < MINIMUM_BITSTREAM_SIZE)
                throw new IllegalArgumentException();
            _abBitstream = abBitstream;
            try {
                resetColumn();
            } catch (MdecException.EndOfStream ex) {
                throw new RuntimeException("Shouldn't happen", ex);
            }
        }

        public void resetColumn() throws MdecException.EndOfStream {
            _iBitsRemaining = 32;
            _iPositionMultOf4 = _iPositionMultOf4 + 4;
            try {
                _iFrontBits = IO.readSInt32LE(_abBitstream, _iPositionMultOf4);
                _iMidBits = IO.readSInt32LE(_abBitstream, _iPositionMultOf4 + 4);
                _iBackBits = IO.readSInt32LE(_abBitstream, _iPositionMultOf4 + 8);
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new MdecException.EndOfStream(ex);
            }
        }

        public int getBits() {
            return _iFrontBits;
        }

        public void skipBits(int iNumBits) {
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("Skipping %d bits '%s'", iNumBits, Misc.bitsToString(_iFrontBits, iNumBits)));
            _iFrontBits = _iFrontBits >>> iNumBits;
            _iBitsRemaining = _iBitsRemaining - iNumBits;
            int i = _iMidBits << (32 - iNumBits);
            _iMidBits = _iMidBits >>> iNumBits;
            _iFrontBits = _iFrontBits | i;

            if (_iBitsRemaining < 0)
                loadMoreBits();
        }

        public void loadMoreBits() {
            _iMidBits = _iBackBits;
            if (_iPositionMultOf4 + 12 < _abBitstream.length)
                _iBackBits = IO.readSInt32LE(_abBitstream, _iPositionMultOf4 + 12);
            else
                _iBackBits = 0;
            _iBitsRemaining = _iBitsRemaining + 32;
            int rTemp = _iMidBits << _iBitsRemaining;
            _iFrontBits = _iFrontBits | rTemp;
            rTemp = 32 - _iBitsRemaining;
            _iMidBits = _iMidBits >>> rTemp;

            _iPositionMultOf4+=4;
        }

    }

}
