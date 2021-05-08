/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson.codecs.pojo.entities.conventions;

import org.bson.codecs.pojo.annotations.BsonCreator;

public final class CreatorNoArgsConstructorModel {
    private Integer integerField;
    private String stringField;
    public long longField;

    @BsonCreator
    public CreatorNoArgsConstructorModel(){
    }

    public CreatorNoArgsConstructorModel(final Integer integerField, final String stringField, final long longField) {
        this.integerField = integerField;
        this.stringField = stringField;
        this.longField = longField;
    }

    public Integer getIntegerField() {
        return integerField;
    }

    public void setIntegerField(final Integer integerField) {
        this.integerField = integerField;
    }

    public String getStringField() {
        return stringField;
    }

    public void setStringField(final String stringField) {
        this.stringField = stringField;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CreatorNoArgsConstructorModel that = (CreatorNoArgsConstructorModel) o;

        if (longField != that.longField) {
            return false;
        }
        if (getIntegerField() != null ? !getIntegerField().equals(that.getIntegerField()) : that.getIntegerField() != null) {
            return false;
        }
        if (getStringField() != null ? !getStringField().equals(that.getStringField()) : that.getStringField() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = getIntegerField() != null ? getIntegerField().hashCode() : 0;
        result = 31 * result + (getStringField() != null ? getStringField().hashCode() : 0);
        result = 31 * result + (int) (longField ^ (longField >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "CreatorNoArgsConstructorModel{"
                + "integerField=" + integerField
                + ", stringField='" + stringField + "'"
                + ", longField=" + longField
                + "}";
    }
}
