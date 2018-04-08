package org.sql2o.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.sql2o.Sql2oException;
import sun.misc.Unsafe;

/**
 * @author mdelapenya
 */
@SuppressWarnings("Unsafe")
public class UnsafeFieldGetterFactory implements FieldGetterFactory, ObjectConstructorFactory {
    private final static Unsafe theUnsafe;
    static {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field declaredField = unsafeClass.getDeclaredField("theUnsafe");
            declaredField.setAccessible(true);
            theUnsafe = (Unsafe) declaredField.get(null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Getter newGetter(final Field field) {
        final Class type = field.getType();
        final boolean isStatic = Modifier.isStatic(field.getModifiers());

        final long offset =  isStatic
                ? theUnsafe.staticFieldOffset(field)
                : theUnsafe.objectFieldOffset(field);

        if (!Modifier.isVolatile(field.getModifiers())) {
            if (type == Boolean.TYPE) {
                return new Getter() {
                    @Override
                    public Object getProperty(Object obj) {
                        return theUnsafe.getBoolean(obj, offset);
                    }

                    @Override
                    public Class getType() {
                        return Boolean.TYPE;
                    }
                };
            }

            if (type == Character.TYPE) {
                return new Getter() {
                    @Override
                    public Object getProperty(Object obj) {
                        return theUnsafe.getChar(obj, offset);
                    }

                    @Override
                    public Class getType() {
                        return Character.TYPE;
                    }
                };
            }

            if (type == Byte.TYPE) {
                return new Getter() {
                    @Override
                    public Object getProperty(Object obj) {
                        return theUnsafe.getByte(obj, offset);
                    }

                    @Override
                    public Class getType() {
                        return Byte.TYPE;
                    }
                };
            }

            if (type == Short.TYPE) {
                return new Getter() {
                    @Override
                    public Object getProperty(Object obj) {
                        return theUnsafe.getShort(obj, offset);
                    }

                    @Override
                    public Class getType() {
                        return Short.TYPE;
                    }
                };
            }

            if (type == Integer.TYPE) {
                return new Getter() {
                    @Override
                    public Object getProperty(Object obj) {
                        return theUnsafe.getInt(obj, offset);
                    }

                    @Override
                    public Class getType() {
                        return Integer.TYPE;
                    }
                };
            }

            if (type == Long.TYPE) {
                return new Getter() {
                    @Override
                    public Object getProperty(Object obj) {
                        return theUnsafe.getLong(obj, offset);
                    }

                    @Override
                    public Class getType() {
                        return Long.TYPE;
                    }
                };
            }

            if (type == Float.TYPE) {
                return new Getter() {
                    @Override
                    public Object getProperty(Object obj) {
                        return theUnsafe.getFloat(obj, offset);
                    }

                    @Override
                    public Class getType() {
                        return Float.TYPE;
                    }
                };
            }
            if (type == Double.TYPE) {
                return new Getter() {
                    @Override
                    public Object getProperty(Object obj) {
                        return theUnsafe.getDouble(obj, offset);
                    }

                    @Override
                    public Class getType() {
                        return Double.TYPE;
                    }
                };
            }
            return new Getter() {
                @Override
                public Object getProperty(Object obj) {
                    return theUnsafe.getObject(obj, offset);
                }

                @Override
                public Class getType() {
                    return type;
                }
            };
        }

        if (type == Boolean.TYPE) {
            return new Getter() {
                @Override
                public Object getProperty(Object obj) {
                    return theUnsafe.getBooleanVolatile(obj, offset);
                }

                @Override
                public Class getType() {
                    return Boolean.TYPE;
                }
            };
        }
        if (type == Character.TYPE) {
            return new Getter() {
                @Override
                public Object getProperty(Object obj) {
                    return theUnsafe.getCharVolatile(obj, offset);
                }

                @Override
                public Class getType() {
                    return Character.TYPE;
                }
            };
        }
        if (type == Byte.TYPE) {
            return new Getter() {
                @Override
                public Object getProperty(Object obj) {
                    return theUnsafe.getByteVolatile(obj, offset);
                }

                @Override
                public Class getType() {
                    return Byte.TYPE;
                }
            };
        }
        if (type == Short.TYPE) {
            return new Getter() {
                @Override
                public Object getProperty(Object obj) {
                    return theUnsafe.getShortVolatile(obj, offset);
                }

                @Override
                public Class getType() {
                    return Short.TYPE;
                }
            };
        }
        if (type == Integer.TYPE) {
            return new Getter() {
                @Override
                public Object getProperty(Object obj) {
                    return theUnsafe.getIntVolatile(obj, offset);
                }

                @Override
                public Class getType() {
                    return Integer.TYPE;
                }
            };
        }
        if (type == Long.TYPE) {
            return new Getter() {
                @Override
                public Object getProperty(Object obj) {
                    return theUnsafe.getLongVolatile(obj, offset);
                }

                @Override
                public Class getType() {
                    return Long.TYPE;
                }
            };
        }
        if (type == Float.TYPE) {
            return new Getter() {
                @Override
                public Object getProperty(Object obj) {
                    return theUnsafe.getFloatVolatile(obj, offset);
                }

                @Override
                public Class getType() {
                    return Float.TYPE;
                }
            };
        }
        if (type == Double.TYPE) {
            return new Getter() {
                @Override
                public Object getProperty(Object obj) {
                    return theUnsafe.getDoubleVolatile(obj, offset);
                }

                @Override
                public Class getType() {
                    return Double.TYPE;
                }
            };
        }
        return new Getter() {
            @Override
            public Object getProperty(Object obj) {
                return theUnsafe.getObjectVolatile(obj, offset);
            }

            @Override
            public Class getType() {
                return type;
            }
        };
    }

    @Override
    public ObjectConstructor newConstructor(final Class<?> clazz) {
        return getConstructor(clazz);
    }
    public static ObjectConstructor getConstructor(final Class<?> clazz) {
        return new ObjectConstructor() {
            @Override
            public Object newInstance() {
                try {
                    return theUnsafe.allocateInstance(clazz);
                } catch (InstantiationException e) {
                    throw new Sql2oException("Could not create a new instance of class " + clazz, e);
                }
            }
        };
    }
}