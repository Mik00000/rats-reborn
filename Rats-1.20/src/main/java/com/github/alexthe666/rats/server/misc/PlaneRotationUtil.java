package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class PlaneRotationUtil {
   private int yawTimer;
   private float yawVariation;
   private int pitchTimer;
   private float pitchVariation;
   private float prevYawVariation;
   private float prevPitchVariation;

   public void resetRotations() {
      this.yawVariation = 0.0F;
      this.pitchVariation = 0.0F;
      this.prevYawVariation = 0.0F;
      this.prevPitchVariation = 0.0F;
   }

   private boolean compareDouble(double a, double b) {
      double c = a - b;
      return Math.abs(c - 1.0) <= 3.0;
   }

   public void calculateChainSwingBuffer(float maxAngle, int bufferTime, float angleDecrement, float divisor, LivingEntity entity) {
      this.prevYawVariation = this.yawVariation;
      if (!this.compareDouble((double)entity.yBodyRot, (double)entity.yBodyRotO) && Mth.abs(this.yawVariation) < maxAngle) {
         this.yawVariation = this.yawVariation + Mth.clamp((entity.yBodyRotO - entity.yBodyRot) / divisor, -maxAngle, maxAngle);
      }

      if (this.yawVariation > angleDecrement) {
         if (this.yawTimer > bufferTime) {
            this.yawVariation -= angleDecrement;
            if (Mth.abs(this.yawVariation) < angleDecrement) {
               this.yawVariation = angleDecrement;
               this.yawTimer = 0;
            }
         } else {
            this.yawTimer++;
         }
      } else if (this.yawVariation < -angleDecrement) {
         if (this.yawTimer > bufferTime) {
            this.yawVariation += angleDecrement;
            if (Mth.abs(this.yawVariation) < angleDecrement) {
               this.yawVariation = angleDecrement;
               this.yawTimer = 0;
            }
         } else {
            this.yawTimer++;
         }
      }
   }

   public void calculateChainPitchBuffer(float maxAngle, int bufferTime, float angleDecrement, float divisor, LivingEntity entity) {
      this.prevPitchVariation = this.pitchVariation;
      if (!this.compareDouble((double)entity.getXRot(), (double)entity.xRotO) && Mth.abs(this.pitchVariation) < maxAngle) {
         this.pitchVariation = this.pitchVariation + Mth.clamp((entity.xRotO - entity.getXRot()) / divisor, -maxAngle, maxAngle);
      }

      if (this.pitchVariation > angleDecrement) {
         if (this.pitchTimer > bufferTime) {
            this.pitchVariation -= angleDecrement;
            if (Mth.abs(this.pitchVariation) < angleDecrement) {
               this.pitchVariation = angleDecrement;
               this.pitchTimer = 0;
            }
         } else {
            this.pitchTimer++;
         }
      } else if (this.pitchVariation < -angleDecrement) {
         if (this.pitchTimer > bufferTime) {
            this.pitchVariation += angleDecrement;
            if (Mth.abs(this.pitchVariation) < angleDecrement) {
               this.pitchVariation = angleDecrement;
               this.pitchTimer = 0;
            }
         } else {
            this.pitchTimer++;
         }
      }
   }

   public void calculateChainWaveBuffer(float maxAngle, int bufferTime, float angleDecrement, float divisor, LivingEntity entity) {
      this.prevPitchVariation = this.pitchVariation;
      if (!(Math.abs(entity.getXRot()) > maxAngle)) {
         if (!this.compareDouble((double)entity.getXRot(), (double)entity.xRotO) && Mth.abs(this.pitchVariation) < maxAngle) {
            this.pitchVariation = this.pitchVariation + Mth.clamp((entity.xRotO - entity.getXRot()) / divisor, -maxAngle, maxAngle);
         }

         if (this.pitchVariation > angleDecrement) {
            if (this.pitchTimer > bufferTime) {
               this.pitchVariation -= angleDecrement;
               if (Mth.abs(this.pitchVariation) < angleDecrement) {
                  this.pitchVariation = 0.0F;
                  this.pitchTimer = 0;
               }
            } else {
               this.pitchTimer++;
            }
         } else if (this.pitchVariation < -angleDecrement) {
            if (this.pitchTimer > bufferTime) {
               this.pitchVariation += angleDecrement;
               if (Mth.abs(this.pitchVariation) < angleDecrement) {
                  this.pitchVariation = 0.0F;
                  this.pitchTimer = 0;
               }
            } else {
               this.pitchTimer++;
            }
         }
      }
   }

   public void calculateChainFlapBuffer(float maxAngle, int bufferTime, float angleDecrement, float divisor, LivingEntity entity) {
      this.prevYawVariation = this.yawVariation;
      if (!this.compareDouble((double)entity.yBodyRot, (double)entity.yBodyRotO) && Mth.abs(this.yawVariation) < maxAngle) {
         this.yawVariation = this.yawVariation + Mth.clamp((entity.yBodyRotO - entity.yBodyRot) / divisor, -maxAngle, maxAngle);
      }

      if (this.yawVariation > angleDecrement) {
         if (this.yawTimer > bufferTime) {
            this.yawVariation -= angleDecrement;
            if (Mth.abs(this.yawVariation) < angleDecrement) {
               this.yawVariation = 0.0F;
               this.yawTimer = 0;
            }
         } else {
            this.yawTimer++;
         }
      } else if (this.yawVariation < -1.0F * angleDecrement) {
         if (this.yawTimer > bufferTime) {
            this.yawVariation += angleDecrement;
            if (Mth.abs(this.yawVariation) < angleDecrement) {
               this.yawVariation = 0.0F;
               this.yawTimer = 0;
            }
         } else {
            this.yawTimer++;
         }
      }
   }

   public void calculateChainSwingBuffer(float maxAngle, int bufferTime, float angleDecrement, LivingEntity entity) {
      this.calculateChainSwingBuffer(maxAngle, bufferTime, angleDecrement, 1.0F, entity);
   }

   public void calculateChainWaveBuffer(float maxAngle, int bufferTime, float angleDecrement, LivingEntity entity) {
      this.calculateChainWaveBuffer(maxAngle, bufferTime, angleDecrement, 1.0F, entity);
   }

   public void calculateChainFlapBuffer(float maxAngle, int bufferTime, float angleDecrement, LivingEntity entity) {
      this.calculateChainFlapBuffer(maxAngle, bufferTime, angleDecrement, 1.0F, entity);
   }

   public void applyChainSwingBuffer(BasicModelPart... boxes) {
      float rotateAmount = (float) (Math.PI / 180.0)
         * Mth.lerp(this.prevYawVariation, this.yawVariation, com.github.alexthe666.rats.client.RatsClientHooks.getPartialTick())
         / (float)boxes.length;

      for (BasicModelPart box : boxes) {
         box.rotateAngleY += rotateAmount;
      }
   }

   public void applyChainWaveBuffer(BasicModelPart... boxes) {
      float rotateAmount = (float) (Math.PI / 180.0)
         * Mth.lerp(this.prevPitchVariation, this.pitchVariation, com.github.alexthe666.rats.client.RatsClientHooks.getPartialTick())
         / (float)boxes.length;

      for (BasicModelPart box : boxes) {
         box.rotateAngleX += rotateAmount;
      }
   }

   public void applyChainFlapBuffer(BasicModelPart... boxes) {
      float rotateAmount = (float) (Math.PI / 180.0)
         * Mth.lerp(com.github.alexthe666.rats.client.RatsClientHooks.getPartialTick(), this.prevYawVariation, this.yawVariation)
         / (float)boxes.length;

      for (BasicModelPart box : boxes) {
         box.rotateAngleZ += rotateAmount;
      }
   }

   public void applyChainFlapBufferReverse(BasicModelPart... boxes) {
      float rotateAmount = (float) (Math.PI / 180.0)
         * Mth.lerp(com.github.alexthe666.rats.client.RatsClientHooks.getPartialTick(), this.prevYawVariation, this.yawVariation)
         / (float)boxes.length;

      for (BasicModelPart box : boxes) {
         box.rotateAngleZ -= rotateAmount * 0.5F;
      }
   }

   public void applyChainSwingBufferReverse(BasicModelPart... boxes) {
      float rotateAmount = (float) (Math.PI / 180.0)
         * Mth.lerp(com.github.alexthe666.rats.client.RatsClientHooks.getPartialTick(), this.prevYawVariation, this.yawVariation)
         / (float)boxes.length;

      for (BasicModelPart box : boxes) {
         box.rotateAngleY -= rotateAmount;
      }
   }

   public void applyChainWaveBufferReverse(BasicModelPart... boxes) {
      float rotateAmount = (float) (Math.PI / 180.0)
         * Mth.lerp(com.github.alexthe666.rats.client.RatsClientHooks.getPartialTick(), this.prevPitchVariation, this.pitchVariation)
         / (float)boxes.length;

      for (BasicModelPart box : boxes) {
         box.rotateAngleX -= rotateAmount;
      }
   }
}
