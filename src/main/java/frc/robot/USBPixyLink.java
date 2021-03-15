package frc.robot;

import io.github.pseudoresonance.pixy2api.Pixy2.Checksum;
import io.github.pseudoresonance.pixy2api.links.Link;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.usb4java.*;

public class USBPixyLink implements Link {
    Context context = null;
    DeviceHandle handle = null;

    public USBPixyLink() {
        open(0);
    }

    @Override
    public int open(int arg) {
        if (handle != null) return 0;
        try {
            context = new Context();
            int result = LibUsb.init(context);
            if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to initialize libusb.", result);

            DeviceList list = new DeviceList();
            result = LibUsb.getDeviceList(null, list);
            if (result < 0) throw new LibUsbException("Unable to get device list", result);

            Device pixyUSB = null;
            try
            {
                // Iterate over all devices and scan for the right one
                for (Device device: list)
                {
                    DeviceDescriptor descriptor = new DeviceDescriptor();
                    result = LibUsb.getDeviceDescriptor(device, descriptor);
                    if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to read device descriptor", result);
                    System.out.print(descriptor.idVendor()); System.out.print(":"); System.out.println(descriptor.idProduct()); 
                    if (descriptor.idVendor() == -20052 /*0xB1AC*/ && descriptor.idProduct() == -4096 /*0xF000*/) {
                        pixyUSB = device;
                        break;
                    }
                }
            }
            finally
            {
                // Ensure the allocated device list is freed
                LibUsb.freeDeviceList(list, true);
            }

            if (pixyUSB == null) return 1;
            handle = new DeviceHandle();
            result = LibUsb.open(pixyUSB, handle);
            if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to open USB device", result);

            result = LibUsb.claimInterface(handle, 1);
            if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to claim interface", result);
            System.out.println("Initialized Pixy");

        } catch (Exception e) {
            System.err.println("Exception while initializing: " + e.getLocalizedMessage());
            throw e;
        }

        return 0;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        LibUsb.releaseInterface(handle, 1);
        LibUsb.close(handle);
        LibUsb.exit(context);
        System.out.println("Closed Pixy");
    }

    @Override
    public int receive(byte[] buffer, int length, Checksum cs) {
        int count = receive(buffer, length);
        if (count < length) return count;
        Checksum c = new Checksum();
        for (int i = 0; i < count; i++) c.updateChecksum(buffer[i]);
        if (c.getChecksum() != cs.getChecksum()) return -1;
        return count;
    }

    @Override
    public int receive(byte[] buffer, int length) {
        ByteBuffer buf = ByteBuffer.allocateDirect(length);
        IntBuffer transferred = IntBuffer.allocate(1);
        int status;
        status = LibUsb.bulkTransfer(handle, (byte)0x82, buf, transferred, 100);
        if (status != LibUsb.SUCCESS) return -1;
        buf.get(buffer, 0, transferred.get(0));
        return transferred.get(0);
    }

    @Override
    public int send(byte[] buffer, int length) {
        ByteBuffer buf = ByteBuffer.allocateDirect(length);
        buf.put(buffer, 0, length);
        IntBuffer transferred = IntBuffer.allocate(1);
        int status = LibUsb.bulkTransfer(handle, (byte)0x02, buf, transferred, 10);
        if (status != LibUsb.SUCCESS) return -1;
        return transferred.get(0);
    }
    
}
