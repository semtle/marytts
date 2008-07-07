/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package marytts.language.en;

import java.util.Iterator;
import java.util.List;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.modules.InternalModule;
import marytts.modules.synthesis.FreeTTSVoices;
import marytts.util.MaryUtils;

import com.sun.speech.freetts.PhoneDurations;
import com.sun.speech.freetts.PhoneDurationsImpl;
import com.sun.speech.freetts.Utterance;
import com.sun.speech.freetts.UtteranceProcessor;
import com.sun.speech.freetts.cart.CARTImpl;
import com.sun.speech.freetts.cart.Durator;



/**
 * Use an individual FreeTTS module for English synthesis.
 *
 * @author Marc Schr&ouml;der
 */

public class FreeTTSDurator extends InternalModule
{
    private UtteranceProcessor processor;

    public FreeTTSDurator()
    {
        super("Durator",
              MaryDataType.get("FREETTS_POSTPROCESSED_EN"),
              MaryDataType.get("FREETTS_DURATIONS_EN")
              );
    }

    public void startup() throws Exception
    {
        super.startup();

        // Initialise FreeTTS
        FreeTTSVoices.load();
        CARTImpl durzCart = new CARTImpl(
            com.sun.speech.freetts.en.us.CMUVoice.class.getResource("durz_cart.txt"));
        PhoneDurations phoneDurations = new PhoneDurationsImpl(
            com.sun.speech.freetts.en.us.CMUVoice.class.getResource("dur_stat.txt"));
        processor = new Durator(durzCart, 150.0f, phoneDurations);
    }

    /**
     * Note: this method contains a synchronized section because it
     * modifies global settings in the utterance.getVoice() object.
     */
    public MaryData process(MaryData d)
    throws Exception
    {
        List utterances = d.getUtterances();
        Iterator it = utterances.iterator();
        while (it.hasNext()) {
            Utterance utterance = (Utterance) it.next();
            // Do we have a rate setting in the utterance?
            String rateString = utterance.getString("rate");
            if (rateString != null && rateString.endsWith("%")) { 
                // yes, need to take rate setting into account
                float originalRate = utterance.getVoice().getRate();
                float rate = originalRate;
                int ratePercent = MaryUtils.getPercentageDelta(rateString);
                if (ratePercent <= -100) ratePercent = -99;
                if (ratePercent >= 300) ratePercent = 300;
                rate = originalRate * (100+ratePercent) / 100;
                synchronized (this) {
                    utterance.getVoice().setRate(rate);
                    processor.processUtterance(utterance);
                    utterance.getVoice().setRate(originalRate);
                }
            } else { // no rate settings to take into account
                processor.processUtterance(utterance);
            }
        }
        MaryData output = new MaryData(outputType());
        output.setUtterances(utterances);
        return output;
    }




}