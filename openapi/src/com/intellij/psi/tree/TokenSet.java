/*
 * Copyright (c) 2004 JetBrains s.r.o. All  Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * -Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduct the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of JetBrains or IntelliJ IDEA
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. JETBRAINS AND ITS LICENSORS SHALL NOT
 * BE LIABLE FOR ANY DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT
 * OF OR RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL JETBRAINS OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN
 * IF JETBRAINS HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */
package com.intellij.psi.tree;

import gnu.trove.THashSet;

import java.util.Arrays;

public class TokenSet {
  private final THashSet<IElementType> mySet = new THashSet<IElementType>(100, (float)0.1);

  public IElementType[] getTypes() {
    return mySet.toArray(new IElementType[mySet.size()]);
  }

  public static TokenSet create(IElementType[] types) {
    TokenSet set = new TokenSet();
    set.mySet.addAll(Arrays.asList(types));
    return set;
  }

  public static TokenSet orSet(TokenSet a, TokenSet b) {
    TokenSet set = new TokenSet();
    set.mySet.addAll(a.mySet);
    set.mySet.addAll(b.mySet);
    return set;
  }

  public static TokenSet andSet(TokenSet a, TokenSet b) {
    TokenSet set = new TokenSet();
    set.mySet.addAll(a.mySet);
    set.mySet.retainAll(b.mySet);
    return set;
  }

  public boolean isInSet(IElementType t) {
    return mySet.contains(t);
  }

}