// Copyright (c) 2017 ChromaWay Inc. See README for license information.

/*
 * Generated by ASN.1 Java Compiler (http://www.asnlab.org/)
 * From ASN.1 module "Messages"
 */
package net.postchain.gtv.messages;

import java.io.*;

import org.asnlab.asndt.runtime.conv.*;
import org.asnlab.asndt.runtime.conv.annotation.*;
import org.asnlab.asndt.runtime.type.AsnType;

public class DictPair {

	@Component(0)
	public String name;

	@Component(1)
	public Gtv value;


	public boolean equals(Object obj) {
		if(!(obj instanceof DictPair)){
			return false;
		}
		return TYPE.equals(this, obj, CONV);
	}

	public void der_encode(OutputStream out) throws IOException {
		TYPE.encode(this, EncodingRules.DISTINGUISHED_ENCODING_RULES, CONV, out);
	}

	public static DictPair der_decode(InputStream in) throws IOException {
		return (DictPair)TYPE.decode(in, EncodingRules.DISTINGUISHED_ENCODING_RULES, CONV);
	}


	public final static AsnType TYPE = Messages.type(65537);

	public final static CompositeConverter CONV;

	static {
		CONV = new AnnotationCompositeConverter(DictPair.class);
		AsnConverter nameConverter = StringConverter.INSTANCE;
		AsnConverter valueConverter = Gtv.CONV;
		CONV.setComponentConverters(new AsnConverter[] { nameConverter, valueConverter });
	}


}